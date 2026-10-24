import api from './api'

export const listTables = (eventId) => api.get(`/event-tables/event/${eventId}`).then(r => r.data)
export const createTable = (body) => api.post('/event-tables', body).then(r => r.data)
export const deleteTable = (id) => api.delete(`/event-tables/${id}`)