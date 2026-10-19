package org.entrypulse.entrypulse.dto.guest;

    public record EventGuestResponse(
            String eventGuestId,
            String eventId,
            String guestId,
            String guestName,
            String eventTableId,
            String seatNumber
    ) {
    }


