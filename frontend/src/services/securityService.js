import api from './api'

export const listSecurity = (eventId) =>
    api.get(`/event-security/event/${eventId}`).then(r => r.data)
export const assignByEmail = (eventId, email) =>
    api.post('/event-security/assign-by-email', { eventId, email }).then(r => r.data)
export const revoke = (id) => api.post(`/event-security/${id}/revoke`).then(r => r.data)
export const reinstate = (id) => api.post(`/event-security/${id}/reinstate`).then(r => r.data)