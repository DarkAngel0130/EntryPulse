<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { listEvents } from '../../services/eventService'
import { getEventSummary } from '../../services/dashboardService'
import { listEventGuests } from '../../services/eventGuestService'
import { listTables } from '../../services/eventTableService'
import { listSecurity } from '../../services/securityService'
import { listCheckIns } from '../../services/checkInService'
import { fmtDate, fmtTime, parseDateTime, fmtClock } from '../../utils/format'

const KEY = 'entrypulse.dashboardEvent'
const QUARTER = 15 * 60 * 1000

const events = ref([])
const selectedId = ref('')
const summary = ref(null)
const guests = ref([])
const tables = ref([])
const security = ref([])
const checkIns = ref([])
const checkInsOk = ref(true)
const loading = ref(true)
const error = ref('')
const updatedAt = ref(null)
const now = ref(Date.now())
let timer = null
let seq = 0

const event = computed(() => events.value.find(e => e.eventId === selectedId.value) || null)
const invited = computed(() => guests.value.length)
const checkedIn = computed(() => summary.value?.totalCheckIns ?? 0)
const invitations = computed(() => summary.value?.totalInvitations ?? 0)
const percent = computed(() => summary.value ? Number(summary.value.attendancePercentage.toFixed(1)) : 0)
const lastQuarter = computed(() =>
    checkIns.value.filter(c => now.value - parseDateTime(c.checkInTime).getTime() <= QUARTER).length)
const activeSecurity = computed(() =>
    security.value.filter(s => s.status !== 'REVOKED' && s.status !== 'DECLINED').length)
const acceptedSecurity = computed(() => security.value.filter(s => s.status === 'ACCEPTED').length)

const bars = computed(() => {
  if (!checkIns.value.length) return []
  const times = checkIns.value.map(c => parseDateTime(c.checkInTime).getTime())
  const floor = (t) => Math.floor(t / QUARTER) * QUARTER
  const start = floor(Math.min(...times))
  const end = floor(Math.max(...times))
  const out = []
  for (let t = start; t <= end; t += QUARTER) out.push({ t, count: 0 })
  for (const x of times) out[(floor(x) - start) / QUARTER].count++
  return out.slice(-16)
})
const maxCount = computed(() => Math.max(1, ...bars.value.map(b => b.count)))

const recent = computed(() =>
    [...checkIns.value]
        .sort((a, b) => parseDateTime(b.checkInTime) - parseDateTime(a.checkInTime))
        .slice(0, 5))

function tableLabel(c) {
  const g = guests.value.find(x => x.guestId === c.guestId)
  const t = g && tables.value.find(x => x.tableId === g.eventTableId)
  return t ? `Table ${t.tableNumber}` : 'No table'
}

function explain(e) {
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (e.response.status === 404) return 'That event could not be found.'
  return 'Something went wrong on the server.'
}

function pickDefault(list) {
  const today = new Date().toLocaleDateString('en-CA')
  const dated = list.filter(e => e.date)
  const live = dated.find(e => e.date === today)
  if (live) return live.eventId
  const upcoming = dated.filter(e => e.date > today).sort((a, b) => a.date.localeCompare(b.date))[0]
  if (upcoming) return upcoming.eventId
  const past = [...dated].sort((a, b) => b.date.localeCompare(a.date))[0]
  return (past || list[0]).eventId
}

function reset() {
  summary.value = null
  guests.value = []
  tables.value = []
  security.value = []
  checkIns.value = []
  checkInsOk.value = true
  error.value = ''
}

async function load(spinner = false) {
  const id = selectedId.value
  if (!id) return
  const mine = ++seq // ignore answers that arrive after the selection changed
  if (spinner) loading.value = true
  const [s, g, t, sec, c] = await Promise.allSettled([
    getEventSummary(id), listEventGuests(id), listTables(id), listSecurity(id), listCheckIns(id),
  ])
  if (mine !== seq) return
  if (s.status === 'fulfilled') { summary.value = s.value; error.value = '' }
  else error.value = explain(s.reason)
  if (g.status === 'fulfilled') guests.value = g.value
  if (t.status === 'fulfilled') tables.value = t.value
  if (sec.status === 'fulfilled') security.value = sec.value
  checkInsOk.value = c.status === 'fulfilled'
  if (c.status === 'fulfilled') checkIns.value = c.value
  now.value = Date.now()
  updatedAt.value = new Date()
  loading.value = false
}

watch(selectedId, (id) => {
  reset()
  try { sessionStorage.setItem(KEY, id) } catch { /* storage unavailable */ }
  load(true)
})

onMounted(async () => {
  try { events.value = await listEvents() }
  catch (e) { error.value = explain(e); loading.value = false; return }
  if (!events.value.length) { loading.value = false; return }
  let saved = ''
  try { saved = sessionStorage.getItem(KEY) || '' } catch { /* ignore */ }
  selectedId.value = events.value.some(e => e.eventId === saved) ? saved : pickDefault(events.value)
  timer = setInterval(() => { if (!document.hidden) load(false) }, 15000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div class="head">
    <h1>Dashboard</h1>
    <div v-if="events.length" class="tools">
      <label class="inline">Event
        <select v-model="selectedId">
          <option v-for="ev in events" :key="ev.eventId" :value="ev.eventId">{{ ev.name }}</option>
        </select>
      </label>
      <span v-if="updatedAt" class="muted small">Updated {{ fmtClock(updatedAt) }}</span>
      <button class="btn btn-secondary" @click="load(false)">Refresh</button>
    </div>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <p v-if="loading" class="muted">Loading dashboard…</p>

  <div v-else-if="!events.length && !error" class="card empty">
    <b>No events yet</b>
    <span class="muted">Create your first event and the dashboard will show its guests and arrivals.</span>
    <RouterLink to="/organiser/events" class="btn" style="align-self: flex-start">Create an event</RouterLink>
  </div>

  <template v-else-if="event && summary">
    <header class="event">
      <h2>{{ event.name }}</h2>
      <p class="muted">{{ fmtDate(event.date) }} · {{ fmtTime(event.time) }} · {{ event.location }}</p>
    </header>

    <div class="grid">
      <div class="card kpi">
        <p class="muted">Invited guests</p>
        <p class="num">{{ invited }}</p>
        <p class="muted small">Registered for this event</p>
      </div>
      <div class="card kpi">
        <p class="muted">Checked in</p>
        <p class="num">{{ checkedIn }}</p>
        <p class="muted small">{{ checkInsOk ? `+${lastQuarter} in the last 15 min` : 'Recent activity unavailable' }}</p>
      </div>
      <div class="card kpi">
        <p class="muted">Attendance rate</p>
        <p class="num">{{ percent }}%</p>
        <p class="muted small">{{ checkedIn }} of {{ invitations }} invitations</p>
      </div>
      <div class="card kpi">
        <p class="muted">Security team</p>
        <p class="num">{{ activeSecurity }}</p>
        <p class="muted small">Assigned · {{ acceptedSecurity }} accepted</p>
      </div>
    </div>

    <div class="panels">
      <section class="card panel">
        <h3>Arrivals per 15 minutes</h3>
        <p v-if="!checkInsOk" class="muted">Check-in data could not be loaded.</p>
        <p v-else-if="!bars.length" class="muted">No check-ins yet. Arrivals appear here as officers check guests in.</p>
        <div v-else class="bars" role="img"
             :aria-label="`Bar chart: ${checkIns.length} check-ins across ${bars.length} intervals of 15 minutes`">
          <div v-for="b in bars" :key="b.t" class="col">
            <span class="count">{{ b.count }}</span>
            <div class="bar" :style="{ height: (b.count / maxCount) * 140 + 'px' }"></div>
            <span class="lbl">{{ fmtClock(new Date(b.t)) }}</span>
          </div>
        </div>
      </section>

      <section class="card panel">
        <h3>Recent check-ins</h3>
        <p v-if="!checkInsOk" class="muted">Check-in data could not be loaded.</p>
        <p v-else-if="!recent.length" class="muted">No check-ins yet.</p>
        <ul v-else class="list">
          <li v-for="(c, i) in recent" :key="i">
            <div>
              <b>{{ c.guestName }}</b>
              <div class="muted small">{{ tableLabel(c) }} · by {{ c.checkedInBy }}</div>
            </div>
            <span class="muted">{{ fmtClock(parseDateTime(c.checkInTime)) }}</span>
          </li>
        </ul>
      </section>
    </div>
  </template>
</template>

<style scoped>
.head h1 { margin: 0; }
.tools { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.inline { flex-direction: row; align-items: center; gap: 8px; font-weight: 500; }
select { font: inherit; padding: 9px 10px; border: 1px solid #cbd3da; border-radius: 6px; background: #fff; }
.event { margin-bottom: 16px; }
.event h2 { margin: 0; font-size: 20px; }
.event p { margin: 2px 0 0; }
.small { font-size: 12px; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; margin-bottom: 12px; }
.kpi { padding: 16px; }
.kpi p { margin: 0; }
.num { font-size: 28px; font-weight: 700; margin: 4px 0; font-variant-numeric: tabular-nums; }
.panels { display: grid; grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr); gap: 12px; }
@media (max-width: 900px) { .panels { grid-template-columns: 1fr; } }
.panel { padding: 16px; }
.panel h3 { margin: 0 0 12px; font-size: 15px; }
.bars { display: flex; align-items: flex-end; gap: 8px; overflow-x: auto; padding-bottom: 4px; }
.col { flex: 1; min-width: 32px; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 6px; }
.count { font-size: 11px; color: var(--muted); font-variant-numeric: tabular-nums; }
.bar { width: 100%; background: var(--accent); border-radius: 3px 3px 0 0; min-height: 2px; }
.lbl { font-size: 11px; color: var(--muted); }
.list { list-style: none; padding: 0; margin: 0; }
.list li { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 10px 0; border-top: 1px solid var(--line); }
.list li:first-child { border-top: 0; padding-top: 0; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 8px; }
</style>