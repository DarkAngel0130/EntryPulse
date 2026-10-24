import api from './api'

export const listEvents = () => api.get('/events').then(r => r.data)
export const createEvent = (e) => api.post('/events', e).then(r => r.data)
export const updateEvent = (id, e) => api.put(`/events/${id}`, e).then(r => r.data)
export const deleteEvent = (id) => api.delete(`/events/${id}`)
export const getEvent = (id) => api.get(`/events/${id}`).then(r => r.data)