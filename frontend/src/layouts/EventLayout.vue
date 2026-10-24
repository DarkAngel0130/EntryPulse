<script setup>
import { ref, computed, provide, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getEvent } from '../services/eventService'
import { fmtDate, fmtTime } from '../utils/format'

const route = useRoute()
const event = ref(null)
const error = ref('')
provide('event', event)

const base = computed(() => `/organiser/events/${route.params.eventId}`)

async function load(id, silent = false) {
  if (!silent) event.value = null
  error.value = ''
  try { event.value = await getEvent(id) }
  catch (e) {
    error.value = !e.response ? 'Cannot reach the server. Is the backend running?'
        : e.response.status === 404 ? 'That event could not be found.'
            : 'Something went wrong on the server.'
  }
}
provide('reloadEvent', () => load(route.params.eventId, true))

watch(() => route.params.eventId, (id) => { if (id) load(id) }, { immediate: true })
</script>

<template>
  <RouterLink to="/organiser/events" class="muted">← Events</RouterLink>

  <p v-if="error" class="error" role="alert" style="margin-top: 12px">{{ error }}</p>
  <p v-else-if="!event" class="muted" style="margin-top: 12px">Loading event…</p>

  <template v-else>
    <header class="top">
      <h1>{{ event.name }}</h1>
      <p class="muted">{{ fmtDate(event.date) }} · {{ fmtTime(event.time) }} · {{ event.location }}</p>
    </header>

    <nav class="tabs" aria-label="Event sections">
      <RouterLink :to="base" exact-active-class="on">Overview</RouterLink>
      <RouterLink :to="`${base}/guests`" active-class="on">Guests</RouterLink>
      <RouterLink :to="`${base}/seating`" active-class="on">Seating</RouterLink>
      <RouterLink :to="`${base}/invitations`" active-class="on">Invitations</RouterLink>
      <RouterLink :to="`${base}/security`" active-class="on">Security</RouterLink>
      <RouterLink :to="`${base}/attendance`" active-class="on">Attendance</RouterLink>
    </nav>

    <RouterView :key="route.params.eventId" />
  </template>
</template>

<style scoped>
.top { margin: 12px 0 16px; }
.top h1 { margin: 0; font-size: 22px; }
.top p { margin: 2px 0 0; }
.tabs { display: flex; gap: 20px; border-bottom: 1px solid var(--line); margin-bottom: 16px; overflow-x: auto; }
.tabs a { padding: 10px 4px; margin-bottom: -1px; color: var(--muted); text-decoration: none;
  font-weight: 500; border-bottom: 2px solid transparent; white-space: nowrap; }
.tabs a:hover { color: var(--ink); }
.tabs a.on { color: var(--accent-dark); border-bottom-color: var(--accent); }
</style>