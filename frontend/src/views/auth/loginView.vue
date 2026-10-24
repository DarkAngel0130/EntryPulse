
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
  <main class="auth-layout">
    <section class="brand-panel">
      <a class="brand" href="/" aria-label="EntryPulse home">
        <span class="brand-icon"></span>
        <span>EntryPulse</span>
      </a>

      <div class="brand-message">
        <h1>Know exactly who is<br class="desktop-break" /> at the door.</h1>
        <p>
          Guest lists, seating and QR check-in for your event,
          in one place.
        </p>
      </div>

      <p class="brand-footer">Events organised. Entrances controlled.</p>
    </section>

    <section class="form-panel">
      <form class="auth-form" @submit.prevent="submit">
        <div class="form-heading">
          <h2>Log in</h2>
          <p>Use your EntryPulse account.</p>
        </div>

        <label for="email">Email</label>
        <input
            id="email"
            v-model="email"
            type="email"
            placeholder="you@example.com"
            autocomplete="username"
            required
        />

        <label for="password">Password</label>
        <div class="password-field">
          <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Enter your password"
              autocomplete="current-password"
              required
          />
          <button
              type="button"
              class="show-button"
              @click="showPassword = !showPassword"
          >
            {{ showPassword ? 'Hide' : 'Show' }}
          </button>
        </div>

        <p v-if="error" class="error" role="alert">
          {{ error }}
        </p>

        <button class="submit-button" type="submit" :disabled="loading">
          {{ loading ? 'Logging in…' : 'Log in' }}
        </button>

        <p class="register-link">
          Don't have an account?
          <RouterLink to="/register">Create an account</RouterLink>
        </p>
      </form>
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f4f6f7;
  color: #192531;
}

.brand-panel {
  background: #0d3f4b;
  color: #fff;
  padding: 68px 9%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 100vh;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  color: #fff;
  text-decoration: none;
  font-size: 21px;
  font-weight: 750;
}

.brand-icon {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: #56bfd3;
}

.brand-message {
  max-width: 480px;
  margin: 80px 0 24px;
}

.brand-message h1 {
  font-size: clamp(32px, 3vw, 44px);
  line-height: 1.15;
  letter-spacing: -1px;
  margin: 0 0 18px;
  font-weight: 750;
}

.brand-message p {
  color: #c0d7de;
  line-height: 1.7;
  font-size: 16px;
  max-width: 430px;
  margin: 0;
}

.brand-footer {
  color: #9fc2cb;
  font-size: 13px;
  margin: 32px 0 0;
}

.form-panel {
  min-height: 100vh;
  padding: 48px 8%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-form {
  width: 100%;
  max-width: 424px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-heading {
  margin-bottom: 10px;
}

.form-heading h2 {
  font-size: 25px;
  margin: 0 0 5px;
  font-weight: 750;
}

.form-heading p {
  color: #687887;
  margin: 0;
  font-size: 15px;
}

.auth-form label {
  font-size: 14px;
  font-weight: 650;
  margin-top: 7px;
}

.auth-form input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 44px;
  padding: 0 13px;
  border: 1px solid #cbd4db;
  border-radius: 7px;
  background: #fff;
  color: #192531;
  font: inherit;
  outline: none;
}

.auth-form input:focus {
  border-color: #137d96;
  box-shadow: 0 0 0 3px rgb(19 125 150 / 12%);
}

.password-field {
  display: flex;
  gap: 9px;
}

.password-field input {
  flex: 1;
}

.show-button {
  padding: 0 15px;
  border: 1px solid #cbd4db;
  border-radius: 7px;
  background: #fff;
  color: #192531;
  cursor: pointer;
}

.show-button:hover {
  background: #edf2f4;
}

.submit-button {
  height: 46px;
  margin-top: 7px;
  border: 0;
  border-radius: 7px;
  background: #117c96;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  background: #0c687f;
}

.submit-button:disabled {
  opacity: 0.65;
  cursor: wait;
}

.error {
  color: #b42318;
  background: #fff0ef;
  border-radius: 6px;
  padding: 10px;
  margin: 0;
  font-size: 14px;
}

.register-link {
  text-align: center;
  margin: 10px 0 0;
  color: #687887;
  font-size: 14px;
}

.register-link a {
  color: #117c96;
  font-weight: 650;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

@media (max-width: 760px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: auto;
    padding: 28px 7%;
    gap: 38px;
  }

  .brand-message {
    margin: 15px 0 0;
  }

  .brand-message h1 {
    font-size: 32px;
  }

  .desktop-break {
    display: none;
  }

  .brand-footer {
    display: none;
  }

  .form-panel {
    min-height: auto;
    padding: 44px 7% 56px;
  }
}
</style>
