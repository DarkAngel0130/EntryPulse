<script setup>
import { useRouter } from 'vue-router'
import { session, clearSession } from '../../auth/session'

const router = useRouter()
function logout() { clearSession(); router.push('/login') }
</script>

<template>
  <main style="padding: 24px">
    <h1>Organiser home</h1>
    <p>Signed in as {{ session.user.name }} ({{ session.user.role }})</p>
    <button class="btn btn-secondary" @click="logout">Log out</button>
  </main>
</template>