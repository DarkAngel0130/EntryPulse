<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listEvents, createEvent } from '../../services/eventService'
import { getEventSummary } from '../../services/dashboardService'
import { fmtDate } from '../../utils/format'

const router = useRouter()
const events = ref([])
const stats = ref({}) // eventId -> summary, or null if it failed to load
const query = ref('')
const loading = ref(true)
const loadError = ref('')
const formOpen = ref(false)
const form = ref({ name: '', date: '', time: '', location: '' })
const saving = ref(false)
const formError = ref('')

const visible = computed(() => {
  const q = query.value.trim().toLowerCase()
  return [...events.value]
      .filter(e => !q || e.name.toLowerCase().includes(q) || (e.location || '').toLowerCase().includes(q))
      .sort((a, b) => (b.date || '').localeCompare(a.date || ''))
})

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 400) return 'Please check the details and try again.'
  if (s === 403) return 'You do not have permission to do that.'
  return 'Something went wrong on the server.'
}

function statusOf(ev) {
  if (!ev.date) return { label: '—', cls: 'no' }
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const day = new Date(ev.date + 'T00:00')
  if (day > today) return { label: 'Upcoming', cls: 'warn' }
  if (day < today) return { label: 'Past', cls: 'no' }
  return { label: 'Live', cls: 'ok' }
}

function num(ev, key) {
  const s = stats.value[ev.eventId]
  if (s === undefined) return '…'
  return s ? s[key] : '—'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try { events.value = await listEvents() }
  catch (e) { loadError.value = explain(e); loading.value = false; return }
  loading.value = false
  const results = await Promise.allSettled(events.value.map(ev => getEventSummary(ev.eventId)))
  const map = {}
  results.forEach((r, i) => { map[events.value[i].eventId] = r.status === 'fulfilled' ? r.value : null })
  stats.value = map
}

function openCreate() {
  form.value = { name: '', date: '', time: '', location: '' }
  formError.value = ''
  formOpen.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  try {
    const created = await createEvent({
      name: form.value.name.trim(),
      date: form.value.date || null,
      time: form.value.time || null,
      location: form.value.location.trim(),
    })
    router.push(`/organiser/events/${created.eventId}`)
  } catch (e) { formError.value = explain(e) }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <h1 class="title">Events</h1>

  <div class="toolbar">
    <input v-model="query" type="search" placeholder="Search events" aria-label="Search events" />
    <button class="btn" @click="openCreate">Create event</button>
  </div>

  <form v-if="formOpen" class="card form" @submit.prevent="save">
    <h2>New event</h2>
    <label>Name <input v-model="form.name" required /></label>
    <div class="two">
      <label>Date <input v-model="form.date" type="date" required /></label>
      <label>Time <input v-model="form.time" type="time" required /></label>
    </div>
    <label>Location <input v-model="form.location" required /></label>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div class="actions">
      <button class="btn" :disabled="saving">{{ saving ? 'Saving…' : 'Create event' }}</button>
      <button type="button" class="btn btn-secondary" @click="formOpen = false">Cancel</button>
    </div>
  </form>

  <p v-if="loading" class="muted">Loading events…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!events.length" class="card empty">
    <b>No events yet</b>
    <span class="muted">Create your first event to start adding guests.</span>
  </div>
  <p v-else-if="!visible.length" class="muted">No events match your search.</p>
  <div v-else class="card tw">
    <table>
      <thead>
      <tr><th>Event</th><th>Date</th><th>Venue</th><th>Invited</th><th>Checked in</th><th>Status</th><th></th></tr>
      </thead>
      <tbody>
      <tr v-for="ev in visible" :key="ev.eventId">
        <td><RouterLink :to="`/organiser/events/${ev.eventId}`" class="name">{{ ev.name }}</RouterLink></td>
        <td>{{ fmtDate(ev.date) }}</td>
        <td>{{ ev.location }}</td>
        <td class="n">{{ num(ev, 'totalInvitations') }}</td>
        <td class="n">{{ num(ev, 'totalCheckIns') }}</td>
        <td><span class="pill" :class="statusOf(ev).cls">{{ statusOf(ev).label }}</span></td>
        <td class="end"><RouterLink :to="`/organiser/events/${ev.eventId}`" class="btn btn-secondary">Manage</RouterLink></td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.title { margin: 0 0 16px; font-size: 22px; }
h2 { margin: 0; font-size: 17px; }
.toolbar { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar input { min-width: 260px; }
.form { padding: 16px; display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; max-width: 520px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.actions { display: flex; gap: 8px; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
.name { color: var(--ink); text-decoration: none; font-weight: 500; }
.name:hover { text-decoration: underline; }
.n { font-variant-numeric: tabular-nums; }
.end { text-align: right; }
</style>