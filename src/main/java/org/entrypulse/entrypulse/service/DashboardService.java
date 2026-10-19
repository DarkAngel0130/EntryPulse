
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.CheckIn;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.repository.CheckInRepository;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.InvitationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final InvitationRepository invitationRepository;
    private final CheckInRepository checkInRepository;

    public DashboardSummary getOrganiserDashboard(String organiserId) {

        List<Event> events = eventRepository.findByOrganiser_Id(organiserId);

        long totalInvitations = 0;
        long totalCheckIns = 0;

        for (Event event : events) {
            totalInvitations += invitationRepository
                    .countByEvent_EventId(event.getEventId());

            totalCheckIns += checkInRepository
                    .countByEventEventId(event.getEventId());
        }

        double attendancePercentage = totalInvitations == 0
                ? 0.0
                : (double) totalCheckIns / totalInvitations * 100.0;

        return new DashboardSummary(
                events.size(),
                totalInvitations,
                totalCheckIns,
                Math.round(attendancePercentage * 100.0) / 100.0
        );
    }

    public EventDashboardSummary getEventDashboard(
            String eventId,
            String organiserId) {

        Event event = eventRepository
                .findByEventIdAndOrganiser_Id(eventId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        long totalInvitations = invitationRepository
                .countByEvent_EventId(event.getEventId());

        long totalCheckIns = checkInRepository
                .countByEventEventId(event.getEventId());

        double attendancePercentage = totalInvitations == 0
                ? 0.0
                : (double) totalCheckIns / totalInvitations * 100.0;

        return new EventDashboardSummary(
                event.getEventId(),
                event.getName(),
                totalInvitations,
                totalCheckIns,
                Math.round(attendancePercentage * 100.0) / 100.0
        );
    }

    public record DashboardSummary(
            long totalEvents,
            long totalInvitations,
            long totalCheckIns,
            double attendancePercentage
    ) {}

    public record EventDashboardSummary(
            String eventId,
            String eventName,
            long totalInvitations,
            long totalCheckIns,
            double attendancePercentage
    ) {}
}
