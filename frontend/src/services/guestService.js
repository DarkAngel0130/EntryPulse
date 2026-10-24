import api from './api'

export const createGuest = (guest) => api.post('/guests', guest).then(r => r.data)
export const updateGuest = (id, guest) => api.put(`/guests/${id}`, guest).then(r => r.data)
export const deleteGuest = (id) => api.delete(`/guests/${id}`)