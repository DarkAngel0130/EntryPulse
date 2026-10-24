<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { createGuest, updateGuest, deleteGuest } from '../../services/guestService'
import { listEventGuests, assignGuest, updateAssignment, removeAssignment } from '../../services/eventGuestService'
import { listInvitations, fetchQr } from '../../services/invitationService'
import { listTables } from '../../services/eventTableService'
import { listCheckIns } from '../../services/checkInService'
import { parseDateTime, fmtClock } from '../../utils/format'

const eventId = useRoute().params.eventId

const guests = ref([])
const invites = ref({})
const tables = ref([])
const checkIns = ref([])
const checkInsOk = ref(true)
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const query = ref('')
const filter = ref('all')
const selectedId = ref('')
const formOpen = ref(false)
const editing = ref(null)
const form = ref({ name: '', seatNumber: '' })
const saving = ref(false)
const formError = ref('')
const qrUrl = ref('')
const qrState = ref('idle')
let qrSeq = 0

const INVITE = {
  ACTIVE: { label: 'Sent', cls: 'info' },
  USED: { label: 'Used', cls: 'no' },
  CANCELLED: { label: 'Cancelled', cls: 'no' },
}

const rows = computed(() => {
  const byGuest = Object.fromEntries(checkIns.value.map(c => [c.guestId, c]))
  return guests.value.map(g => {
    const inv = invites.value[g.eventGuestId] || null
    const ci = byGuest[g.guestId] || null
    const table = tables.value.find(t => t.tableId === g.eventTableId)
    return {
      ...g, inv, ci,
      tableNumber: table ? table.tableNumber : null,
      arrived: !!ci || inv?.status === 'USED',
    }
  })
})

const counts = computed(() => ({
  all: rows.value.length,
  in: rows.value.filter(r => r.arrived).length,
  out: rows.value.filter(r => !r.arrived).length,
}))

const visible = computed(() => {
  const q = query.value.trim().toLowerCase()
  return rows.value
      .filter(r => filter.value === 'all' || (filter.value === 'in' ? r.arrived : !r.arrived))
      .filter(r => !q || r.guestName.toLowerCase().includes(q))
      .sort((a, b) => a.guestName.localeCompare(b.guestName))
})

const selected = computed(() => rows.value.find(r => r.eventGuestId === selectedId.value) || null)
const qrKey = computed(() => (selected.value?.inv?.status === 'ACTIVE' ? selected.value.inv.invitationId : ''))

const inviteOf = (r) => (r.inv ? INVITE[r.inv.status] || { label: r.inv.status, cls: 'no' } : { label: 'Not sent', cls: 'no' })
const timeOf = (r) => (r.ci ? fmtClock(parseDateTime(r.ci.checkInTime)) : '—')

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 400) return 'Please check the details and try again.'
  if (s === 403) return 'You do not have permission to do that.'
  if (s === 404) return 'That event or guest could not be found.'
  if (s === 409) return 'This guest is already assigned to this event, or the table is full.'
  return 'Something went wrong on the server.'
}


async function load(spinner = true) {
  if (spinner) loading.value = true
  loadError.value = ''

  try {
    const [g, inv, t, c] = await Promise.allSettled([
      listEventGuests(eventId),
      listInvitations(eventId),
      listTables(eventId),
      listCheckIns(eventId),
    ])

    // The guest list is essential.
    if (g.status === 'rejected') throw g.reason

    guests.value = g.value

    // Invitation information is optional for displaying guests.
    if (inv.status === 'fulfilled') {
      invites.value = Object.fromEntries(
          inv.value.map(i => [
            i.eventGuest?.eventGuestId ?? i.eventGuestId,
            {
              ...i,
              invitationId: i.id ?? i.invitationId,
              eventGuestId: i.eventGuest?.eventGuestId ?? i.eventGuestId,
              guestName: i.guest?.name ?? i.guestName
            }
          ])
      )
    } else {
      invites.value = {}
    }

    // Table information is optional.
    if (t.status === 'fulfilled') {
      tables.value = t.value
    }

    // Check-in information is optional.
    checkInsOk.value = c.status === 'fulfilled'

    if (c.status === 'fulfilled') {
      checkIns.value = c.value
    } else {
      checkIns.value = []
    }

  } catch (e) {
    loadError.value = explain(e)
  } finally {
    loading.value = false
  }
}


function clearQr() {
  qrSeq++
  if (qrUrl.value) URL.revokeObjectURL(qrUrl.value)
  qrUrl.value = ''
  qrState.value = 'idle'
}

watch(qrKey, async (id) => {
  clearQr()
  if (!id) return
  const mine = qrSeq
  qrState.value = 'loading'
  try {
    const url = await fetchQr(id)
    if (mine !== qrSeq) { URL.revokeObjectURL(url); return }
    qrUrl.value = url
    qrState.value = 'ok'
  } catch { if (mine === qrSeq) qrState.value = 'error' }
})

function openAdd() {
  editing.value = null
  form.value = { name: '', seatNumber: '' }
  formError.value = ''
  formOpen.value = true
}

function openEdit(r) {
  editing.value = r
  form.value = { name: r.guestName, seatNumber: r.seatNumber || '' }
  formError.value = ''
  formOpen.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  const name = form.value.name.trim()
  const seatNumber = form.value.seatNumber.trim() || null
  try {
    if (editing.value) {
      const r = editing.value
      if (name !== r.guestName) await updateGuest(r.guestId, { name })
      // The PUT replaces the assignment, so send the current table back or it is cleared.
      await updateAssignment(r.eventGuestId, { eventTableId: r.eventTableId, seatNumber })
    } else {
      const guest = await createGuest({ name })
      try {
        await assignGuest({ eventId, guestId: guest.id, seatNumber })
      } catch (err) {
        await deleteGuest(guest.id).catch(() => {})
        throw err
      }
    }
    formOpen.value = false
    await load(false)
  } catch (e) { formError.value = explain(e) }
  finally { saving.value = false }
}

async function remove(r) {
  if (!confirm(`Remove ${r.guestName} from this event?`)) return
  actionError.value = ''
  try {
    await removeAssignment(r.eventGuestId)
    selectedId.value = ''
    await load(false)
  } catch (e) {
    const s = e.response?.status
    actionError.value = (s === 409 || s === 500)
        ? 'This guest could not be removed. They may already have an invitation or a check-in.'
        : explain(e)
  }
}

onMounted(() => load())
onBeforeUnmount(clearQr)
</script>

<template>
  <div class="toolbar">
    <div class="left">
      <input v-model="query" type="search" placeholder="Search guests" aria-label="Search guests" />
      <button class="btn" :class="{ 'btn-secondary': filter !== 'all' }" @click="filter = 'all'">All ({{ counts.all }})</button>
      <button class="btn" :class="{ 'btn-secondary': filter !== 'in' }" @click="filter = 'in'">Checked in ({{ counts.in }})</button>
      <button class="btn" :class="{ 'btn-secondary': filter !== 'out' }" @click="filter = 'out'">Not arrived ({{ counts.out }})</button>
    </div>
    <div class="left">
      <button class="btn btn-secondary" @click="load(false)">Refresh</button>
      <button class="btn" @click="openAdd">Add guest</button>
    </div>
  </div>

  <form v-if="formOpen" class="card form" @submit.prevent="save">
    <h2>{{ editing ? 'Edit guest' : 'New guest' }}</h2>
    <label>Name <input v-model="form.name" required /></label>
    <label>Seat (optional) <input v-model="form.seatNumber" maxlength="20" /></label>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div class="actions">
      <button class="btn" :disabled="saving">{{ saving ? 'Saving…' : 'Save' }}</button>
      <button type="button" class="btn btn-secondary" @click="formOpen = false">Cancel</button>
    </div>
  </form>

  <p v-if="actionError" class="error" role="alert" style="margin: 12px 0">{{ actionError }}</p>
  <p v-if="!checkInsOk && !loading" class="muted note">Check-in details could not be loaded, so arrival times are unavailable.</p>

  <p v-if="loading" class="muted">Loading guests…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!guests.length" class="card empty">
    <b>No guests yet</b>
    <span class="muted">Add the first guest for this event.</span>
  </div>

  <div v-else class="layout">
    <div class="main">
      <p v-if="!visible.length" class="muted">No guests match your search or filter.</p>
      <div v-else class="card tw">
        <table>
          <thead><tr><th>Guest</th><th>Table</th><th>Seat</th><th>Invitation</th><th>Status</th><th>Time</th></tr></thead>
          <tbody>
          <tr v-for="r in visible" :key="r.eventGuestId" class="row" :class="{ on: r.eventGuestId === selectedId }"
              tabindex="0" @click="selectedId = r.eventGuestId" @keydown.enter="selectedId = r.eventGuestId">
            <td><b>{{ r.guestName }}</b></td>
            <td>{{ r.tableNumber ?? '—' }}</td>
            <td>{{ r.seatNumber || '—' }}</td>
            <td><span class="pill" :class="inviteOf(r).cls">{{ inviteOf(r).label }}</span></td>
            <td><span class="pill" :class="r.arrived ? 'ok' : 'no'">{{ r.arrived ? 'Checked in' : 'Not arrived' }}</span></td>
            <td>{{ timeOf(r) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <aside v-if="selected" class="card panel" aria-label="Guest details">
      <div class="top">
        <h3>{{ selected.guestName }}</h3>
        <span class="pill" :class="selected.arrived ? 'ok' : 'no'">{{ selected.arrived ? 'Checked in' : 'Not arrived' }}</span>
      </div>

      <div class="qr">
        <img v-if="qrState === 'ok'" :src="qrUrl" alt="Invitation QR code" width="170" height="170" />
        <span v-else-if="qrState === 'loading'" class="muted">Loading QR code…</span>
        <span v-else-if="qrState === 'error'" class="error">The QR code could not be loaded.</span>
        <span v-else-if="!selected.inv" class="muted">No invitation yet.<br />Create one on the Invitations tab.</span>
        <span v-else class="muted">Invitation {{ inviteOf(selected).label.toLowerCase() }}.<br />No QR code to show.</span>
      </div>
      <a v-if="qrUrl" class="btn btn-secondary" :href="qrUrl" :download="`invitation-${selected.guestName}.png`">Download QR</a>

      <dl>
        <dt class="muted">Table / seat</dt>
        <dd>{{ selected.tableNumber ?? '—' }} / {{ selected.seatNumber || '—' }}</dd>
        <dt class="muted">Invitation</dt>
        <dd>{{ inviteOf(selected).label }}</dd>
        <dt class="muted">Checked in</dt>
        <dd>{{ selected.ci ? `${timeOf(selected)} by ${selected.ci.checkedInBy}` : '—' }}</dd>
      </dl>

      <div class="actions">
        <button class="btn btn-secondary" @click="openEdit(selected)">Edit guest</button>
        <button class="btn btn-secondary danger" @click="remove(selected)">Remove</button>
      </div>
    </aside>
  </div>
</template>

<style scoped>
h2 { margin: 0; font-size: 17px; }
.toolbar { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.left { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.left input { min-width: 220px; }
.form { padding: 16px; display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; max-width: 520px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
.note { font-size: 12px; margin: 0 0 8px; }
.layout { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.main { flex: 1; min-width: 420px; }
.row { cursor: pointer; }
.row:hover, .row.on { background: #f0f7f9; }
.row:focus-visible { outline: 2px solid var(--accent); outline-offset: -2px; }
.panel { width: 300px; flex-shrink: 0; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.top h3 { margin: 0; font-size: 16px; }
.qr { min-height: 170px; display: grid; place-items: center; text-align: center; border: 1px dashed #cbd3da; border-radius: 8px; padding: 8px; }
dl { display: grid; grid-template-columns: auto 1fr; gap: 6px 12px; margin: 0; font-size: 13px; }
dd { margin: 0; }
</style>