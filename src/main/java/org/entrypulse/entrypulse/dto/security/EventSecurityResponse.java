
package org.entrypulse.entrypulse.dto.security;

import org.entrypulse.entrypulse.model.EventSecurity;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventSecurityResponse(
        String assignmentId,
        String eventId,
        String eventName,
        LocalDate eventDate,
        LocalTime eventTime,
        String eventLocation,
        String userId,
        String name,
        String email,
        String status
) {

    public static EventSecurityResponse from(EventSecurity a) {
        return new EventSecurityResponse(
                a.getId(),
                a.getEvent().getEventId(),
                a.getEvent().getName(),
                a.getEvent().getDate(),
                a.getEvent().getTime(),
                a.getEvent().getLocation(),
                a.getUser().getId(),
                a.getUser().getName(),
                a.getUser().getEmail(),
                a.getStatus().name()
        );
    }
}
