<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { listCheckIns } from '../../services/checkInService'

const eventId = useRoute().params.eventId
const rows = ref([])
const loading = ref(true)
const error = ref('')
const updatedAt = ref(null)
let timer = null

const fmt = (t) => new Date(t).toLocaleString('en-ZA',
    { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

async function load(spinner = false) {
  if (spinner) loading.value = true
  try {
    rows.value = await listCheckIns(eventId)
    error.value = ''
    updatedAt.value = new Date()
  } catch (e) {
    error.value = !e.response ? 'Cannot reach the server. Is the backend running?'
        : 'Could not load the check-ins for this event.'
  } finally { loading.value = false }
}

onMounted(() => {
  load(true)
  timer = setInterval(() => { if (!document.hidden) load() }, 15000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div class="head">
    <p class="muted" style="margin: 0">
      {{ rows.length }} checked in<span v-if="updatedAt"> · updated {{ updatedAt.toLocaleTimeString('en-ZA') }}</span>
    </p>
    <button class="btn btn-secondary" @click="load()">Refresh</button>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <p v-if="loading" class="muted">Loading…</p>
  <div v-else-if="!rows.length && !error" class="card empty">
    <b>No check-ins yet</b>
    <span class="muted">Guests appear here as security officers check them in. This page refreshes every 15 seconds.</span>
  </div>
  <div v-else-if="rows.length" class="card tw">
    <table>
      <thead><tr><th>Guest</th><th>Checked in</th><th>By</th></tr></thead>
      <tbody>
      <tr v-for="(r, i) in rows" :key="i">
        <td><b>{{ r.guestName }}</b></td>
        <td>{{ fmt(r.checkInTime) }}</td>
        <td>{{ r.checkedInBy }}</td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.empty { padding: 24px; display: flex; flex-direction: column; gap: 4px; }
</style>