import api from './api'

export const listInvitations = (eventId) =>
    api.get(`/invitations/event/${eventId}`).then(r => r.data)

export const createInvitation = (eventGuestId) =>
    api.post('/invitations', null, { params: { eventGuestId } }).then(r => r.data)

export const cancelInvitation = (id) =>
    api.post(`/invitations/${id}/cancel`).then(r => r.data)

export const fetchQr = (id) =>
    api.get(`/qr-codes/invitations/${id}`, { responseType: 'blob' })
        .then(r => URL.createObjectURL(r.data))

export const reinstateInvitation = (id) =>
    api.post(`/invitations/${id}/reinstate`).then(r => r.data)