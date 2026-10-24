import api from './api'

export const listEventGuests = (eventId) => api.get(`/event-guests/event/${eventId}`).then(r => r.data)
export const assignGuest = (body) => api.post('/event-guests', body).then(r => r.data)
export const updateAssignment = (id, body) => api.put(`/event-guests/${id}`, body).then(r => r.data)
export const removeAssignment = (id) => api.delete(`/event-guests/${id}`)