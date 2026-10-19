<script setup>
import { useRouter } from 'vue-router'
import { session, clearSession } from '../auth/session'

const router = useRouter()
function logout() { clearSession(); router.push('/login') }
</script>

<template>
  <div class="shell">
    <nav class="side" aria-label="Main">
      <div class="brand"><span class="logo"></span>EntryPulse</div>
      <RouterLink to="/organiser" exact-active-class="on">Dashboard</RouterLink>
      <RouterLink to="/organiser/events" active-class="on">Events</RouterLink>
      <div class="who">
        <b>{{ session.user.name }}</b>
        <span>Organiser</span>
        <button class="btn btn-secondary" @click="logout">Log out</button>
      </div>
    </nav>
    <main class="content"><RouterView /></main>
  </div>
</template>

<style scoped>
.shell { display: flex; min-height: 100vh; }
.side { width: 208px; flex-shrink: 0; background: #fff; border-right: 1px solid var(--line);
  padding: 16px 10px; display: flex; flex-direction: column; gap: 2px; }
.brand { font-weight: 700; font-size: 16px; padding: 4px 10px 16px; display: flex; align-items: center; gap: 8px; }
.logo { width: 18px; height: 18px; border-radius: 5px; background: var(--accent); }
.side a { padding: 9px 10px; border-radius: 6px; color: #4a5b6a; text-decoration: none; font-weight: 500; }
.side a:hover { background: #f0f3f6; }
.side a.on { background: #e1f0f4; color: var(--accent-dark); }
.who { margin-top: auto; padding: 10px; display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: var(--muted); }
.who b { color: var(--ink); }
.content { flex: 1; min-width: 0; padding: 24px; }
@media (max-width: 760px) { .side { display: none; } .content { padding: 16px; } }
</style>