
package org.entrypulse.entrypulse.dto.checkin;

import java.time.LocalDateTime;

public record InvitationValidationResponse(
        String status,
        String guestName,
        String eventName,
        String checkedInBy,
        LocalDateTime checkInTime,
        Integer tableNumber,
        String seatNumber
) {
    public static InvitationValidationResponse invalid() {
        return new InvitationValidationResponse(
                "INVALID_FOR_EVENT",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
