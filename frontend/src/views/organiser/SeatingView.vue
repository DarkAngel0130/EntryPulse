<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listTables, createTable, deleteTable } from '../../services/eventTableService'
import { listEventGuests, updateAssignment } from '../../services/eventGuestService'

const eventId = useRoute().params.eventId

const tables = ref([])
const guests = ref([])
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const formOpen = ref(false)
const form = ref({ tableNumber: 1, capacity: 8 })
const saving = ref(false)
const formError = ref('')
const managing = ref(null)
const pick = ref('')
const busy = ref(false)

const sortedTables = computed(() => [...tables.value].sort((a, b) => a.tableNumber - b.tableNumber))
const unseated = computed(() =>
    guests.value.filter(g => !g.eventTableId).sort((a, b) => a.guestName.localeCompare(b.guestName)))
const byTable = computed(() => {
  const map = {}
  for (const g of guests.value) if (g.eventTableId) (map[g.eventTableId] ||= []).push(g)
  return map
})
const seatedAt = (t) => byTable.value[t.tableId] || []
const percent = (t) => Math.min(100, Math.round((seatedAt(t).length / t.capacity) * 100))
const managed = computed(() => tables.value.find(t => t.tableId === managing.value) || null)
const managedFull = computed(() => managed.value && seatedAt(managed.value).length >= managed.value.capacity)

function explain(e, ctx = '') {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 400) return 'Please check the numbers and try again.'
  if (s === 403) return 'You do not have permission to do that.'
  if (s === 404) return 'That event, table or guest could not be found.'
  if (s === 409) return ctx === 'create' ? 'A table with that number already exists.'
      : ctx === 'seat' ? 'That table is full.'
          : ctx === 'delete' ? 'Move or remove the guests at this table first.'
              : 'That conflicts with existing data.'
  return 'Something went wrong on the server.'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [t, g] = await Promise.all([listTables(eventId), listEventGuests(eventId)])
    tables.value = t
    guests.value = g
  } catch (e) { loadError.value = explain(e) }
  finally { loading.value = false }
}

function openAdd() {
  const max = tables.value.reduce((m, t) => Math.max(m, t.tableNumber), 0)
  form.value = { tableNumber: max + 1, capacity: 8 }
  formError.value = ''
  formOpen.value = true
}

async function addTable() {
  saving.value = true
  formError.value = ''
  try {
    await createTable({ eventId, tableNumber: form.value.tableNumber, capacity: form.value.capacity })
    formOpen.value = false
    await load()
  } catch (e) { formError.value = explain(e, 'create') }
  finally { saving.value = false }
}

function replaceGuest(updated) {
  guests.value = guests.value.map(g => (g.eventGuestId === updated.eventGuestId ? updated : g))
}

async function move(g, tableId) {
  busy.value = true
  actionError.value = ''
  try {
    // Send the seat back too, because the PUT replaces the whole assignment.
    replaceGuest(await updateAssignment(g.eventGuestId, { eventTableId: tableId, seatNumber: g.seatNumber }))
    pick.value = ''
  } catch (e) { actionError.value = explain(e, 'seat') }
  finally { busy.value = false }
}

async function seatPicked() {
  const g = guests.value.find(x => x.eventGuestId === pick.value)
  if (g && managed.value) await move(g, managed.value.tableId)
}

async function removeTable(t) {
  if (!confirm(`Delete Table ${t.tableNumber}? This cannot be undone.`)) return
  actionError.value = ''
  try {
    await deleteTable(t.tableId)
    managing.value = null
    await load()
  } catch (e) { actionError.value = explain(e, 'delete') }
}

onMounted(load)
</script>

<template>
  <div class="head">
    <div class="status">
      <span v-if="!loading && unseated.length" class="pill warn">
        {{ unseated.length }} guest{{ unseated.length === 1 ? '' : 's' }} without a table
      </span>
      <span v-else-if="!loading && guests.length" class="muted">All guests have a table.</span>
    </div>
    <button class="btn" @click="openAdd">Add table</button>
  </div>

  <form v-if="formOpen" class="card form" @submit.prevent="addTable">
    <h2>New table</h2>
    <div class="two">
      <label>Table number <input v-model.number="form.tableNumber" type="number" min="1" required /></label>
      <label>Seats <input v-model.number="form.capacity" type="number" min="1" required /></label>
    </div>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div class="actions">
      <button class="btn" :disabled="saving">{{ saving ? 'Saving…' : 'Add table' }}</button>
      <button type="button" class="btn btn-secondary" @click="formOpen = false">Cancel</button>
    </div>
  </form>

  <p v-if="actionError" class="error" role="alert" style="margin: 12px 0">{{ actionError }}</p>

  <section v-if="managed" class="card manage" aria-label="Manage table">
    <div class="row">
      <h2>Table {{ managed.tableNumber }}</h2>
      <span class="muted">{{ seatedAt(managed).length }} of {{ managed.capacity }} seats assigned</span>
      <button class="btn btn-secondary" @click="managing = null">Close</button>
    </div>

    <p v-if="!seatedAt(managed).length" class="muted">Nobody is seated at this table yet.</p>
    <ul v-else class="list">
      <li v-for="g in seatedAt(managed)" :key="g.eventGuestId">
        <span>{{ g.guestName }}<span v-if="g.seatNumber" class="muted"> · seat {{ g.seatNumber }}</span></span>
        <button class="btn btn-secondary" :disabled="busy" @click="move(g, null)">Unseat</button>
      </li>
    </ul>

    <div class="seat">
      <select v-model="pick" aria-label="Guest to seat" :disabled="managedFull || !unseated.length">
        <option value="">{{ managedFull ? 'Table is full' : unseated.length ? 'Choose a guest…' : 'No unseated guests' }}</option>
        <option v-for="g in unseated" :key="g.eventGuestId" :value="g.eventGuestId">{{ g.guestName }}</option>
      </select>
      <button class="btn" :disabled="!pick || busy" @click="seatPicked">Seat here</button>
      <button class="btn btn-secondary danger" @click="removeTable(managed)">Delete table</button>
    </div>
  </section>

  <p v-if="loading" class="muted">Loading seating…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!tables.length" class="card empty">
    <b>No tables yet</b>
    <span class="muted">Add a table, then seat your guests at it.</span>
  </div>
  <div v-else class="grid">
    <div v-for="t in sortedTables" :key="t.tableId" class="card table">
      <div class="row">
        <b>Table {{ t.tableNumber }}</b>
        <span v-if="seatedAt(t).length >= t.capacity" class="pill no">Full</span>
        <span v-else-if="!seatedAt(t).length" class="pill no">Empty</span>
      </div>
      <div class="bar" role="progressbar" :aria-label="`Table ${t.tableNumber} occupancy`"
           aria-valuemin="0" aria-valuemax="100" :aria-valuenow="percent(t)">
        <span :style="{ width: percent(t) + '%' }"></span>
      </div>
      <p class="muted">{{ seatedAt(t).length }} of {{ t.capacity }} seats assigned</p>
      <button class="btn btn-secondary" style="align-self: flex-start" @click="managing = t.tableId; pick = ''">Manage</button>
    </div>
  </div>
</template>

<style scoped>
h2 { margin: 0; font-size: 17px; }
.status { min-height: 24px; }
.form, .manage { padding: 16px; display: flex; flex-direction: column; gap: 12px; margin: 16px 0; max-width: 560px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.actions { display: flex; gap: 8px; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-top: 16px; }
.table { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.table p { margin: 0; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.bar { height: 6px; background: var(--line); border-radius: 3px; }
.bar span { display: block; height: 100%; background: var(--accent); border-radius: 3px; }
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.list li { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.seat { display: flex; gap: 8px; flex-wrap: wrap; }
select { font: inherit; padding: 9px 10px; border: 1px solid #cbd3da; border-radius: 6px; background: #fff; min-width: 200px; }
</style>