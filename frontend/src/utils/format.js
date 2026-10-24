export const fmtDate = (d) => d
    ? new Date(d + 'T00:00').toLocaleDateString('en-ZA', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
    : '—'

export const fmtTime = (t) => (t ? t.slice(0, 5) : '—')

// The server sends LocalDateTime with up to 9 fractional digits; trim to 3 so every browser parses it.
export const parseDateTime = (s) => new Date(s.replace(/(\.\d{3})\d+$/, '$1'))

export const fmtClock = (d) =>
    d.toLocaleTimeString('en-ZA', { hour: '2-digit', minute: '2-digit' })