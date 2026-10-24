import api from './api'

export const listCheckIns = (eventId) =>
    api.get(`/check-ins/event/${eventId}`).then(r => r.data)