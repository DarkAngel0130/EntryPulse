
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../services/authService'
import { setSession } from '../../auth/session'

const router = useRouter()

const name = ref('')
const email = ref('')
const phoneNumber = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''

  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }

  loading.value = true

  try {
    const data = await register({
      name: name.value.trim(),
      email: email.value.trim(),
      password: password.value,
      phoneNumber: phoneNumber.value.trim()
    })

    if (data.role !== 'ORGANISER') {
      error.value = 'Registration did not return an organiser account.'
      return
    }

    setSession(data)
    await router.push('/organiser')
  } catch (e) {
    error.value = e.response
        ? (e.response.data?.message || 'Could not create your account. Please check your details.')
        : 'Cannot reach the server. Is the backend running?'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-layout">
    <section class="brand-panel">
      <RouterLink class="brand" to="/login">
        <span class="brand-icon"></span>
        <span>EntryPulse</span>
      </RouterLink>

      <div class="brand-message">
        <h1>Bring every event<br class="desktop-break" /> under control.</h1>
        <p>
          Manage your guest lists, seating arrangements and
          QR check-in from one place.
        </p>
      </div>

      <p class="brand-footer">Events organised. Entrances controlled.</p>
    </section>

    <section class="form-panel">
      <form class="auth-form" @submit.prevent="submit">
        <div class="form-heading">
          <h2>Create account</h2>
          <p>Get started with EntryPulse.</p>
        </div>

        <label for="name">Full name</label>
        <input
            id="name"
            v-model="name"
            type="text"
            placeholder="Your full name"
            autocomplete="name"
            required
        />

        <label for="email">Email</label>
        <input
            id="email"
            v-model="email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
            required
        />

        <label for="phone">Phone number <span class="optional">(optional)</span></label>
        <input
            id="phone"
            v-model="phoneNumber"
            type="tel"
            placeholder="Your phone number"
            autocomplete="tel"
        />

        <label for="password">Password</label>
        <input
            id="password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="Create a password"
            autocomplete="new-password"
            required
        />

        <label for="confirmPassword">Confirm password</label>
        <div class="password-field">
          <input
              id="confirmPassword"
              v-model="confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Enter your password again"
              autocomplete="new-password"
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
          {{ loading ? 'Creating account…' : 'Create account' }}
        </button>

        <p class="register-link">
          Already have an account?
          <RouterLink to="/login">Log in</RouterLink>
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
  margin: 60px 0 24px;
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
  padding: 40px 8%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-form {
  width: 100%;
  max-width: 424px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.form-heading {
  margin-bottom: 8px;
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
  margin-top: 6px;
}

.optional {
  color: #687887;
  font-weight: 400;
}

.auth-form input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 42px;
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
  margin-top: 8px;
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
  margin: 8px 0 0;
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
    gap: 32px;
  }

  .brand-message {
    margin: 12px 0 0;
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
    padding: 36px 7% 48px;
  }
}
</style>
