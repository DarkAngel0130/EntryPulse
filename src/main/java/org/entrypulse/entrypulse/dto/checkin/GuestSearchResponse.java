
package org.entrypulse.entrypulse.dto.checkin;

public record GuestSearchResponse(
        String guestId,
        String guestName,
        String eventGuestId,
        String seatNumber,
        boolean alreadyCheckedIn,
        String checkedInBy,
        String checkInTime
) {}
