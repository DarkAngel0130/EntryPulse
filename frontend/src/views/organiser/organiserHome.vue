<script setup>
import { useRouter } from 'vue-router'
import { session, clearSession } from '../../auth/session'

const router = useRouter()
function logout() { clearSession(); router.push('/login') }
</script>

<template>
  <h1 style="margin: 0 0 8px">Dashboard</h1>
  <p class="muted" style="color: var(--muted)">Attendance statistics will appear here.</p>
</template>