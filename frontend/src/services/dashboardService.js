import api from './api'

export const getOverview = () => api.get('/dashboard').then(r => r.data)
export const getEventSummary = (eventId) => api.get(`/dashboard/events/${eventId}`).then(r => r.data)