<script setup>
import { ref, computed, inject, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getEventSummary } from '../../services/dashboardService'
import { updateEvent, deleteEvent } from '../../services/eventService'
import { fmtDate, fmtTime } from '../../utils/format'

const router = useRouter()
const event = inject('event')
const reloadEvent = inject('reloadEvent')

const summary = ref(null)
const error = ref('')
const editing = ref(false)
const form = ref({ name: '', date: '', time: '', location: '' })
const saving = ref(false)
const formError = ref('')
const actionError = ref('')

const percent = computed(() =>
    summary.value ? Number(summary.value.attendancePercentage.toFixed(1)) : 0)

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 400) return 'Please check the details and try again.'
  if (s === 404) return 'That event no longer exists.'
  return 'Something went wrong on the server.'
}

function startEdit() {
  const ev = event.value
  form.value = { name: ev.name, date: ev.date || '', time: (ev.time || '').slice(0, 5), location: ev.location || '' }
  formError.value = ''
  editing.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  try {
    await updateEvent(event.value.eventId, {
      name: form.value.name.trim(),
      date: form.value.date || null,
      time: form.value.time || null,
      location: form.value.location.trim(),
    })
    await reloadEvent()
    editing.value = false
  } catch (e) { formError.value = explain(e) }
  finally { saving.value = false }
}

async function remove() {
  if (!confirm(`Delete "${event.value.name}"? This cannot be undone.`)) return
  actionError.value = ''
  try {
    await deleteEvent(event.value.eventId)
    router.push('/organiser/events')
  } catch (e) {
    const s = e.response?.status
    actionError.value = !e.response ? explain(e)
        : (s === 409 || s === 500)
            ? 'The event could not be deleted. It may still have guests, invitations or check-ins.'
            : explain(e)
  }
}

onMounted(async () => {
  try { summary.value = await getEventSummary(event.value.eventId) }
  catch (e) { error.value = !e.response ? explain(e) : 'Could not load the numbers for this event.' }
})
</script>

<template>
  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <div v-if="summary" class="grid">
    <div class="card kpi"><p class="muted">Invitations</p><p class="num">{{ summary.totalInvitations }}</p></div>
    <div class="card kpi"><p class="muted">Checked in</p><p class="num">{{ summary.totalCheckIns }}</p></div>
    <div class="card kpi"><p class="muted">Attendance rate</p><p class="num">{{ percent }}%</p></div>
  </div>

  <form v-if="editing" class="card details" @submit.prevent="save">
    <h2>Edit event</h2>
    <label>Name <input v-model="form.name" required /></label>
    <div class="two">
      <label>Date <input v-model="form.date" type="date" required /></label>
      <label>Time <input v-model="form.time" type="time" required /></label>
    </div>
    <label>Location <input v-model="form.location" required /></label>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div class="actions">
      <button class="btn" :disabled="saving">{{ saving ? 'Saving…' : 'Save' }}</button>
      <button type="button" class="btn btn-secondary" @click="editing = false">Cancel</button>
    </div>
  </form>

  <section v-else class="card details">
    <h2>Event details</h2>
    <dl>
      <dt class="muted">Date</dt><dd>{{ fmtDate(event.date) }}</dd>
      <dt class="muted">Time</dt><dd>{{ fmtTime(event.time) }}</dd>
      <dt class="muted">Location</dt><dd>{{ event.location }}</dd>
    </dl>
    <div class="actions">
      <button class="btn btn-secondary" @click="startEdit">Edit event</button>
      <button class="btn btn-secondary danger" @click="remove">Delete event</button>
    </div>
    <p v-if="actionError" class="error" role="alert">{{ actionError }}</p>
  </section>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; margin-bottom: 16px; }
.kpi { padding: 16px; } .kpi p { margin: 0; }
.num { font-size: 28px; font-weight: 700; margin: 4px 0; font-variant-numeric: tabular-nums; }
.details { padding: 16px; max-width: 520px; display: flex; flex-direction: column; gap: 12px; }
h2 { margin: 0; font-size: 16px; }
dl { display: grid; grid-template-columns: 100px 1fr; gap: 8px 12px; margin: 0; }
dd { margin: 0; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.actions { display: flex; gap: 8px; }
</style>