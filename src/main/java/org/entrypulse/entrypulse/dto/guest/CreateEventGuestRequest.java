
package org.entrypulse.entrypulse.dto.guest;

import jakarta.validation.constraints.NotBlank;

public record CreateEventGuestRequest(
        @NotBlank(message = "Event ID is required")
        String eventId,

        @NotBlank(message = "Guest ID is required")
        String guestId,

        String eventTableId,
        String seatNumber
) {
}

