<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { listEventGuests } from '../../services/eventGuestService'
import { listInvitations, createInvitation, cancelInvitation, reinstateInvitation, fetchQr } from '../../services/invitationService'
import { getEvent } from '../../services/eventService'
import { jsPDF } from 'jspdf'

const eventId = useRoute().params.eventId
const event = ref(null)

const rows = ref([])
const invites = ref({})
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const busy = ref(false)
const qr = ref(null)

const sorted = computed(() =>
    [...rows.value].sort((a, b) => a.guestName.localeCompare(b.guestName)))
const missing = computed(() => rows.value.filter(r => !invites.value[r.eventGuestId]))

const STATUS = {
  ACTIVE: { label: 'Active', cls: 'ok' },
  CANCELLED: { label: 'Cancelled', cls: 'no' },
  USED: { label: 'Used', cls: 'info' },
}
function statusOf(r) {
  const i = invites.value[r.eventGuestId]
  if (!i) return { label: 'No invitation', cls: 'no' }
  return STATUS[i.status] || { label: i.status, cls: 'no' }
}

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 403) return 'You do not have permission to do that.'
  if (s === 404) return 'That event or invitation could not be found.'
  if (s === 409) return 'That conflicts with the invitation\'s current state.'
  return 'Something went wrong on the server.'
}

function normaliseInvitation(inv) {
  return {
    ...inv,
    invitationId: inv.id ?? inv.invitationId,
    eventGuestId: inv.eventGuest?.eventGuestId ?? inv.eventGuestId,
    guestName: inv.guest?.name ?? inv.guestName
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [eventData, list, invs] = await Promise.all([
      getEvent(eventId),
      listEventGuests(eventId),
      listInvitations(eventId)
    ])
    event.value = eventData
    rows.value = list
    invites.value = Object.fromEntries(
        invs.map(raw => {
          const inv = normaliseInvitation(raw)
          return [inv.eventGuestId, inv]
        })
    )
  } catch (e) { loadError.value = explain(e) }
  finally { loading.value = false }
}

async function makeInvite(r) {
  actionError.value = ''
  try {
    const inv = normaliseInvitation(
        await createInvitation(r.eventGuestId)
    )
    invites.value = { ...invites.value, [inv.eventGuestId]: inv }
  } catch (e) { actionError.value = explain(e) }
}

async function createAll() {
  busy.value = true
  actionError.value = ''
  let failed = 0
  for (const r of missing.value) {
    try {
      const inv = normaliseInvitation(
          await createInvitation(r.eventGuestId)
      )
      invites.value = { ...invites.value, [inv.eventGuestId]: inv }
    } catch { failed++ }
  }
  if (failed) actionError.value = `${failed} invitation${failed === 1 ? '' : 's'} could not be created.`
  busy.value = false
}

async function cancelInvite(r) {
  if (!confirm(`Cancel the invitation for ${r.guestName}? Their QR code will stop working.`)) return
  actionError.value = ''
  try {
    const inv = normaliseInvitation(
        await cancelInvitation(invites.value[r.eventGuestId].invitationId)
    )
    invites.value = { ...invites.value, [inv.eventGuestId]: inv }
    closeQr()
  } catch (e) { actionError.value = explain(e) }
}

async function reinstateInvite(r) {
  if (!confirm(`Reinstate the invitation for ${r.guestName}? A new QR code will be generated.`)) {
    return
  }

  actionError.value = ''

  try {
    const raw = await reinstateInvitation(
        invites.value[r.eventGuestId].invitationId
    )

    const inv = normaliseInvitation(raw)

    invites.value = {
      ...invites.value,
      [inv.eventGuestId]: inv
    }

    closeQr()
  } catch (e) {
    actionError.value = explain(e)
  }
}

async function showQr(r) {
  actionError.value = ''
  closeQr()
  try {
    const url = await fetchQr(invites.value[r.eventGuestId].invitationId)
    qr.value = { guestName: r.guestName, url }
  } catch (e) { actionError.value = explain(e) }
}

function loadImage(url) {
  return new Promise((resolve, reject) => {
    const image = new Image()

    image.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = image.naturalWidth
        canvas.height = image.naturalHeight

        const context = canvas.getContext('2d')
        context.drawImage(image, 0, 0)

        resolve(canvas.toDataURL('image/png'))
      } catch (error) {
        reject(error)
      }
    }

    image.onerror = () => reject(new Error('Failed to load QR code image.'))
    image.src = url
  })
}

function closeQr() {
  if (qr.value) URL.revokeObjectURL(qr.value.url)
  qr.value = null
}

async function downloadInvitationPdf(r) {
  actionError.value = ''

  const invitation = invites.value[r.eventGuestId]

  if (!invitation || invitation.status !== 'ACTIVE') {
    actionError.value = 'Only active invitations can be downloaded.'
    return
  }

  if (!event.value) {
    actionError.value = 'Event details are not available.'
    return
  }

  let qrUrl

  try {
    qrUrl = await fetchQr(invitation.invitationId)

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    })

    // Heading
    pdf.setFontSize(22)
    pdf.text('EntryPulse', 105, 25, { align: 'center' })

    pdf.setFontSize(16)
    pdf.text('Event Invitation', 105, 36, { align: 'center' })

    // Guest details
    pdf.setFontSize(12)
    pdf.text(`Guest: ${r.guestName}`, 20, 55)

    // Seating assignment
    const tableNumber = r.eventTable?.tableNumber
    const seatNumber = r.seatNumber

    pdf.text(
        `Table Number: ${tableNumber ?? 'Not assigned'}`,
        20,
        65
    )

    pdf.text(
        `Seat Number: ${seatNumber ?? 'Not assigned'}`,
        20,
        72
    )




    // Event details
    pdf.setFontSize(14)
    pdf.text('Event Details', 20, 87)

    pdf.setFontSize(11)
    pdf.text(`Event: ${event.value.name || 'N/A'}`, 20, 98)
    pdf.text(`Date: ${event.value.date || 'To be confirmed'}`, 20, 107)
    pdf.text(`Time: ${event.value.time || 'To be confirmed'}`, 20, 116)

    const locationLines = pdf.splitTextToSize(
        `Location: ${event.value.location || 'To be confirmed'}`,
        170
    )
    pdf.text(locationLines, 20, 110)

    // QR code
    const qrImage = await loadImage(qrUrl)

    pdf.addImage(qrImage, 'PNG', 65, 130, 80, 80)

    // Instructions
    pdf.setFontSize(10)
    pdf.text(
        'Present this QR code at the entrance for verification.',
        105,
        225,
        { align: 'center' }
    )

    pdf.setFontSize(9)
    pdf.text(
        'Keep this invitation secure. Anyone holding the QR code may be able to use it.',
        105,
        235,
        { align: 'center', maxWidth: 170 }
    )

    const safeName = r.guestName.replace(/[^a-z0-9_-]/gi, '_')
    pdf.save(`EntryPulse-Invitation-${safeName}.pdf`)
  } catch (e) {
    actionError.value = 'Could not generate the invitation PDF. Please try again.'
    console.error(e)
  } finally {
    if (qrUrl) URL.revokeObjectURL(qrUrl)
  }
}



onMounted(load)
onBeforeUnmount(closeQr)
</script>

<template>
  <div class="head">
    <p class="muted" style="margin: 0">
      {{ rows.length - missing.length }} of {{ rows.length }} guest{{ rows.length === 1 ? '' : 's' }} have an invitation
    </p>
    <button v-if="missing.length" class="btn" :disabled="busy" @click="createAll">
      {{ busy ? 'Creating…' : `Create all (${missing.length})` }}
    </button>
  </div>

  <div v-if="qr" class="card qr">
    <img :src="qr.url" alt="Invitation QR code" width="220" height="220" />
    <div>
      <h2>{{ qr.guestName }}</h2>
      <p class="muted">Send this QR code to the guest. Anyone who holds it can use the invitation.</p>
      <div class="actions">
        <a class="btn" :href="qr.url" :download="`invitation-${qr.guestName}.png`">Download</a>
        <button class="btn btn-secondary" @click="closeQr">Close</button>
      </div>
    </div>
  </div>

  <p v-if="actionError" class="error" role="alert" style="margin: 12px 0">{{ actionError }}</p>

  <p v-if="loading" class="muted">Loading invitations…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!rows.length" class="card empty">
    <b>No guests yet</b>
    <span class="muted">Add guests on the Guests tab first, then create their invitations here.</span>
  </div>
  <div v-else class="card tw">
    <table>
      <thead><tr><th>Guest</th><th>Invitation</th><th></th></tr></thead>
      <tbody>
      <tr v-for="r in sorted" :key="r.eventGuestId">
        <td><b>{{ r.guestName }}</b></td>
        <td><span class="pill" :class="statusOf(r).cls">{{ statusOf(r).label }}</span></td>
        <td>
          <div class="row-actions">
            <button v-if="!invites[r.eventGuestId]" class="btn btn-secondary" @click="makeInvite(r)">Create invitation</button>
            <template v-else-if="invites[r.eventGuestId].status === 'ACTIVE'">
              <button class="btn btn-secondary" @click="showQr(r)">
                View QR
              </button>

              <button class="btn btn-secondary" @click="downloadInvitationPdf(r)">
                Download PDF
              </button>

              <button class="btn btn-secondary danger" @click="cancelInvite(r)">
                Cancel
              </button>
            </template>
            <button
                v-if="invites[r.eventGuestId]?.status === 'CANCELLED'"
                class="btn btn-secondary"
                @click="reinstateInvite(r)"
            >
              Reinstate
            </button>
          </div>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
h2 { margin: 0; font-size: 17px; }
.actions { display: flex; gap: 8px; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
.qr { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; padding: 16px; margin: 16px 0; }
.qr .muted { margin: 4px 0 12px; max-width: 360px; }
</style>