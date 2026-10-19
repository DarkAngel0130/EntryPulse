package org.entrypulse.entrypulse.dto.guest;

import jakarta.validation.constraints.Size;

public record UpdateEventGuestRequest(
        String eventTableId,

        @Size(max = 20, message = "Seat number cannot exceed 20 characters")
        String seatNumber
) {
}