<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listSecurity, assignByEmail, revoke, reinstate } from '../../services/securityService'
import { listCheckIns } from '../../services/checkInService'
import { parseDateTime, fmtClock } from '../../utils/format'

const eventId = useRoute().params.eventId

const rows = ref([])
const checkIns = ref([])
const checkInsOk = ref(true)
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const email = ref('')
const adding = ref(false)
const addError = ref('')

const STATUS = {
  PENDING: { label: 'Pending', cls: 'warn' },
  ACCEPTED: { label: 'Accepted', cls: 'ok' },
  DECLINED: { label: 'Declined', cls: 'no' },
  REVOKED: { label: 'Revoked', cls: 'bad' },
}
const statusOf = (s) => STATUS[s] || { label: s, cls: 'no' }

const stats = computed(() => {
  const map = {}
  for (const c of checkIns.value) {
    const t = parseDateTime(c.checkInTime).getTime()
    const s = (map[c.checkedInById] ||= { count: 0, last: 0 })
    s.count++
    if (t > s.last) s.last = t
  }
  return map
})
const countOf = (r) => (checkInsOk.value ? (stats.value[r.userId]?.count ?? 0) : '—')
const lastOf = (r) => {
  const s = stats.value[r.userId]
  return checkInsOk.value && s ? fmtClock(new Date(s.last)) : '—'
}

const sorted = computed(() => [...rows.value].sort((a, b) => a.name.localeCompare(b.name)))
const assigned = computed(() => rows.value.filter(r => r.status !== 'REVOKED' && r.status !== 'DECLINED').length)

function explain(e) {
  const s = e.response?.status
  if (!e.response) return 'Cannot reach the server. Is the backend running?'
  if (s === 403) return 'You do not have permission to do that.'
  if (s === 404) return 'That event or assignment could not be found.'
  if (s === 409) return 'That action is not allowed in the assignment\'s current state.'
  return 'Something went wrong on the server.'
}

async function load(spinner = true) {
  if (spinner) loading.value = true
  loadError.value = ''
  const [sec, c] = await Promise.allSettled([listSecurity(eventId), listCheckIns(eventId)])
  if (sec.status === 'fulfilled') rows.value = sec.value
  else loadError.value = explain(sec.reason)
  checkInsOk.value = c.status === 'fulfilled'
  if (c.status === 'fulfilled') checkIns.value = c.value
  loading.value = false
}

async function add() {
  adding.value = true
  addError.value = ''
  try {
    await assignByEmail(eventId, email.value.trim())
    email.value = ''
    await load(false)
  } catch (e) {
    const s = e.response?.status
    addError.value = !e.response ? explain(e)
        : s === 404 ? 'No security account was found with that email.'
            : s === 409 ? 'That officer is already assigned to this event.'
                : s === 400 ? 'Enter a valid email address.'
                    : explain(e)
  } finally { adding.value = false }
}

function replace(updated) {
  rows.value = rows.value.map(r => (r.assignmentId === updated.assignmentId ? updated : r))
}

async function revokeOne(r) {
  if (!confirm(`Revoke ${r.name}? They will no longer be able to check guests in for this event.`)) return
  actionError.value = ''
  try { replace(await revoke(r.assignmentId)) }
  catch (e) { actionError.value = explain(e) }
}

async function reinstateOne(r) {
  actionError.value = ''
  try { replace(await reinstate(r.assignmentId)) }
  catch (e) { actionError.value = explain(e) }
}

onMounted(() => load())
</script>

<template>
  <div class="head">
    <div class="title">
      <h2>Security team</h2>
      <span v-if="!loading" class="pill ok">{{ assigned }} assigned</span>
    </div>
    <button class="btn btn-secondary" @click="load(false)">Refresh</button>
  </div>

  <form class="card add" @submit.prevent="add">
    <label>Assign a security officer by email
      <span class="inline">
        <input v-model="email" type="email" placeholder="officer@example.com" required />
        <button class="btn" :disabled="adding">{{ adding ? 'Assigning…' : 'Assign' }}</button>
      </span>
    </label>
    <p v-if="addError" class="error" role="alert">{{ addError }}</p>
    <p class="muted hint">The officer must accept in the EntryPulse mobile app. Only accepted officers can check guests in.</p>
  </form>

  <p v-if="actionError" class="error" role="alert" style="margin: 12px 0">{{ actionError }}</p>
  <p v-if="!checkInsOk && !loading" class="muted hint">Check-in counts could not be loaded.</p>

  <p v-if="loading" class="muted">Loading…</p>
  <p v-else-if="loadError" class="error" role="alert">{{ loadError }}</p>
  <div v-else-if="!rows.length" class="card empty">
    <b>No security officers assigned</b>
    <span class="muted">Enter an officer's email above to assign them to this event.</span>
  </div>
  <template v-else>
    <div class="card tw">
      <table>
        <thead><tr><th>Officer</th><th>Status</th><th>Check-ins</th><th>Last scan</th><th></th></tr></thead>
        <tbody>
        <tr v-for="r in sorted" :key="r.assignmentId">
          <td><b>{{ r.name }}</b><div class="muted small">{{ r.email }}</div></td>
          <td><span class="pill" :class="statusOf(r.status).cls">{{ statusOf(r.status).label }}</span></td>
          <td class="n">{{ countOf(r) }}</td>
          <td class="n">{{ lastOf(r) }}</td>
          <td>
            <div class="row-actions">
              <button v-if="r.status !== 'REVOKED'" class="btn btn-secondary danger" @click="revokeOne(r)">Revoke</button>
              <button v-else class="btn btn-secondary" @click="reinstateOne(r)">Reinstate</button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <p class="muted hint">Revoking ends an officer's access to this event immediately. Reinstating sets them back to Pending, so they must accept again.</p>
  </template>
</template>

<style scoped>
.title { display: flex; align-items: center; gap: 10px; }
.title h2 { margin: 0; font-size: 18px; }
.add { padding: 16px; display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; max-width: 560px; }
.inline { display: flex; gap: 8px; } .inline input { flex: 1; }
.hint { font-size: 12px; margin: 8px 0 0; }
.small { font-size: 12px; font-weight: 400; }
.n { font-variant-numeric: tabular-nums; }
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
</style>