<script setup>
import { ref, onMounted } from 'vue'
import { listEvents, createEvent, updateEvent, deleteEvent } from '../../services/eventService'

const events = ref([])
const loading = ref(true)
const loadError = ref('')
const formOpen = ref(false)
const editingId = ref(null)
const form = ref({ name: '', date: '', time: '', location: '' })
const saving = ref(false)
const formError = ref('')

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 400) return 'Please check the details and try again.'
  if (s === 403) return 'You do not have permission to do that.'
  if (s === 404) return 'That event no longer exists.'
  return 'Something went wrong on the server.'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try { events.value = await listEvents() }
  catch (e) { loadError.value = explain(e) }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', date: '', time: '', location: '' }
  formError.value = ''
  formOpen.value = true
}

function openEdit(ev) {
  editingId.value = ev.eventId
  form.value = { name: ev.name, date: ev.date || '', time: (ev.time || '').slice(0, 5), location: ev.location || '' }
  formError.value = ''
  formOpen.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  const body = {
    name: form.value.name.trim(),
    date: form.value.date || null,
    time: form.value.time || null,
    location: form.value.location.trim(),
  }
  try {
    if (editingId.value) await updateEvent(editingId.value, body)
    else await createEvent(body)
    formOpen.value = false
    await load()
  } catch (e) { formError.value = explain(e) }
  finally { saving.value = false }
}

async function remove(ev) {
  if (!confirm(`Delete "${ev.name}"? This cannot be undone.`)) return
  try { await deleteEvent(ev.eventId); await load() }
  catch (e) { loadError.value = explain(e) }
}

const fmtDate = (d) => d
    ? new Date(d + 'T00:00').toLocaleDateString('en-ZA', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
    : '—'

onMounted(load)
</script>

<template>
  <div class="head">
    <h1>Events</h1>
    <button class="btn" @click="openCreate">Create event</button>
  </div>

  <form v-if="formOpen" class="card form" @submit.prevent="save">
    <h2>{{ editingId ? 'Edit event' : 'New event' }}</h2>
    <label>Name <input v-model="form.name" required /></label>
    <div class="two">
      <label>Date <input v-model="form.date" type="date" required /></label>
      <label>Time <input v-model="form.time" type="time" required /></label>
    </div>
    <label>Location <input v-model="form.location" required /></label>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div class="actions">
      <button class="btn" :disabled="saving">{{ saving ? 'Saving…' : 'Save' }}</button>
      <button type="button" class="btn btn-secondary" @click="formOpen = false">Cancel</button>
    </div>
  </form>

  <p v-if="loading" class="muted">Loading events…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!events.length" class="card empty">
    <b>No events yet</b>
    <span class="muted">Create your first event to start adding guests.</span>
  </div>
  <div v-else class="card tw">
    <table>
      <thead><tr><th>Event</th><th>Date</th><th>Time</th><th>Location</th><th></th></tr></thead>
      <tbody>
      <tr v-for="ev in events" :key="ev.eventId">
        <td><b>{{ ev.name }}</b></td>
        <td>{{ fmtDate(ev.date) }}</td>
        <td>{{ ev.time ? ev.time.slice(0, 5) : '—' }}</td>
        <td>{{ ev.location }}</td>
        <td class="row-actions">
          <button class="btn btn-secondary" @click="openEdit(ev)">Edit</button>
          <button class="btn btn-secondary danger" @click="remove(ev)">Delete</button>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h1 { margin: 0; font-size: 22px; } h2 { margin: 0; font-size: 17px; }
.card { background: #fff; border: 1px solid var(--line); border-radius: 8px; }
.form { padding: 16px; display: flex; flex-direction: column; gap: 12px; margin-bottom: 16px; max-width: 520px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.actions { display: flex; gap: 8px; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
.muted { color: var(--muted); } .error { color: var(--danger); margin: 0; }
.tw { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 12px; color: var(--muted); padding: 9px 14px; background: var(--bg); }
td { padding: 10px 14px; border-top: 1px solid var(--line); }
.row-actions { display: flex; gap: 8px; justify-content: flex-end; }
.danger { color: var(--danger); }
</style>