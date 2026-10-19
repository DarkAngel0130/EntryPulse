<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../services/authService'
import { setSession } from '../../auth/session'

const router = useRouter()
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const data = await login(email.value.trim(), password.value)
    if (data.role !== 'ORGANISER') {
      error.value = 'Security accounts use the EntryPulse mobile app.'
      return
    }
    setSession(data)
    router.push('/organiser')
  } catch (e) {
    error.value = e.response
        ? (e.response.data?.message || 'Incorrect email or password.')
        : 'Cannot reach the server. Is the backend running?'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="wrap">
    <form class="card" @submit.prevent="submit">
      <h1>Log in</h1>
      <p class="muted">Use your EntryPulse account.</p>
      <label>Email
        <input v-model="email" type="email" autocomplete="username" required />
      </label>
      <label>Password
        <span class="pw">
          <input v-model="password" :type="showPassword ? 'text' : 'password'"
                 autocomplete="current-password" required />
          <button type="button" class="btn btn-secondary" @click="showPassword = !showPassword">
            {{ showPassword ? 'Hide' : 'Show' }}
          </button>
        </span>
      </label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button class="btn" :disabled="loading">{{ loading ? 'Logging in…' : 'Log in' }}</button>
    </form>
  </main>
</template>

<style scoped>
.wrap { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.card { width: 100%; max-width: 380px; background: var(--surface); border: 1px solid var(--line);
  border-radius: 10px; padding: 28px; display: flex; flex-direction: column; gap: 16px; }
h1 { margin: 0; font-size: 22px; }
.muted { margin: 0; color: var(--muted); }
.pw { display: flex; gap: 8px; } .pw input { flex: 1; }
.error { margin: 0; color: var(--danger); }
</style>