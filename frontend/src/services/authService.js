import api from './api'

export async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password })
    return data // { token, userId, name, role }
}

export async function register({ name, email, password, phoneNumber }) {
    const { data } = await api.post('/auth/register', {
        name,
        email,
        password,
        phoneNumber: phoneNumber || null
    })

    return data
}