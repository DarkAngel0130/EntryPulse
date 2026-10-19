import axios from 'axios'
import { session, clearSession } from '../auth/session'

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL })

api.interceptors.request.use((config) => {
    const token = session.user?.token
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

api.interceptors.response.use(
    (response) => response,
    (error) => {
        const isAuthCall = error.config?.url?.startsWith('/auth/')
        if (error.response?.status === 401 && !isAuthCall) {
            clearSession()
            window.location.assign('/login')
        }
        return Promise.reject(error)
    }
)

export default api