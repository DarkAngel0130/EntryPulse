import { reactive } from 'vue'

const KEY = 'entrypulse.session'

function load() {
    try { return JSON.parse(sessionStorage.getItem(KEY)) } catch { return null }
}

// { token, userId, name, role } or null
export const session = reactive({ user: load() })

export function setSession(data) {
    session.user = data
    sessionStorage.setItem(KEY, JSON.stringify(data))
}

export function clearSession() {
    session.user = null
    sessionStorage.removeItem(KEY)
}

export const isAuthenticated = () => !!session.user?.token