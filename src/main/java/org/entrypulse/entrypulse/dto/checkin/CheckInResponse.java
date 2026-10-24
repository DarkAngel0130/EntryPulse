package org.entrypulse.entrypulse.dto.checkin;

import org.entrypulse.entrypulse.model.CheckIn;
import java.time.LocalDateTime;

public record CheckInResponse(String guestId, String guestName, String eventName,
                              String checkedInById, String checkedInBy,
                              LocalDateTime checkInTime) {
    public static CheckInResponse from(CheckIn c) {
        return new CheckInResponse(
                c.getGuest().getId(), c.getGuest().getName(), c.getEvent().getName(),
                c.getCheckedInBy().getId(), c.getCheckedInBy().getName(), c.getCheckInTime());
    }
}