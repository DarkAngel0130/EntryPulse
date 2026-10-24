
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.model.User;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.EventSecurityRepository;
import org.entrypulse.entrypulse.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventSecurityService {

    private final EventSecurityRepository eventSecurityRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventSecurity assignSecurityToEvent(
            String eventId,
            String securityUserId,
            String organiserId) {

        User organiser = getUser(organiserId);
        requireRole(organiser, User.Role.ORGANISER);

        Event event = eventRepository
                .findByEventIdAndOrganiser_Id(eventId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        User securityUser = getUser(securityUserId);
        requireRole(securityUser, User.Role.SECURITY);

        if (eventSecurityRepository
                .existsByEvent_EventIdAndUser_Id(eventId, securityUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This security user already has an assignment for this event");
        }

        EventSecurity assignment = new EventSecurity();
        assignment.setEvent(event);
        assignment.setUser(securityUser);
        assignment.setStatus(EventSecurity.AssignmentStatus.PENDING);

        return eventSecurityRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<EventSecurity> getSecurityForEvent(
            String eventId,
            String organiserId) {

        User organiser = getUser(organiserId);
        requireRole(organiser, User.Role.ORGANISER);

        // Verify ownership even when the event has no assignments.
        eventRepository.findByEventIdAndOrganiser_Id(eventId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        return eventSecurityRepository
                .findByEvent_EventIdAndEvent_Organiser_Id(
                        eventId, organiserId);
    }

    @Transactional(readOnly = true)
    public List<EventSecurity> getMyAssignments(String securityUserId) {
        User user = getUser(securityUserId);
        requireRole(user, User.Role.SECURITY);

        return eventSecurityRepository.findByUser_Id(securityUserId);
    }

    public EventSecurity acceptAssignment(
            String assignmentId,
            String securityUserId) {

        return respondToAssignment(
                assignmentId,
                securityUserId,
                EventSecurity.AssignmentStatus.ACCEPTED);
    }

    public EventSecurity declineAssignment(
            String assignmentId,
            String securityUserId) {

        return respondToAssignment(
                assignmentId,
                securityUserId,
                EventSecurity.AssignmentStatus.DECLINED);
    }

    private EventSecurity respondToAssignment(
            String assignmentId,
            String securityUserId,
            EventSecurity.AssignmentStatus newStatus) {

        User user = getUser(securityUserId);
        requireRole(user, User.Role.SECURITY);

        EventSecurity assignment = eventSecurityRepository
                .findByIdAndUser_Id(assignmentId, securityUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        if (assignment.getStatus()
                != EventSecurity.AssignmentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending assignments can be accepted or declined");
        }

        assignment.setStatus(newStatus);
        assignment.setRespondedAt(LocalDateTime.now());

        return eventSecurityRepository.save(assignment);
    }

    public EventSecurity revokeAssignment(
            String assignmentId,
            String organiserId) {

        User organiser = getUser(organiserId);
        requireRole(organiser, User.Role.ORGANISER);

        EventSecurity assignment = eventSecurityRepository
                .findByIdAndEvent_Organiser_Id(
                        assignmentId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        if (assignment.getStatus()
                == EventSecurity.AssignmentStatus.REVOKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Assignment is already revoked");
        }

        assignment.setStatus(
                EventSecurity.AssignmentStatus.REVOKED);

        return eventSecurityRepository.save(assignment);
    }

    public EventSecurity reinstateAssignment(
            String assignmentId,
            String organiserId) {

        User organiser = getUser(organiserId);
        requireRole(organiser, User.Role.ORGANISER);

        EventSecurity assignment = eventSecurityRepository
                .findByIdAndEvent_Organiser_Id(
                        assignmentId, organiserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        if (assignment.getStatus()
                != EventSecurity.AssignmentStatus.REVOKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only revoked assignments can be reinstated");
        }

        assignment.setStatus(EventSecurity.AssignmentStatus.PENDING);
        assignment.setRespondedAt(null);

        return eventSecurityRepository.save(assignment);
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private void requireRole(User user, User.Role requiredRole) {
        if (user.getRole() != requiredRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }
    public EventSecurity assignSecurityByEmail(
            String eventId, String email, String organiserId) {

        User target = userRepository.findByEmail(email.trim())
                .filter(u -> u.getRole() == User.Role.SECURITY)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No security account found with that email"));

        return assignSecurityToEvent(eventId, target.getId(), organiserId);
    }
}
