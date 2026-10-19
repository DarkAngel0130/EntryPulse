
package org.entrypulse.entrypulse.service;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.dto.guest.CreateEventGuestRequest;
import org.entrypulse.entrypulse.dto.guest.EventGuestResponse;
import org.entrypulse.entrypulse.dto.guest.UpdateEventGuestRequest;
import org.entrypulse.entrypulse.model.Event;
import org.entrypulse.entrypulse.model.EventGuest;
import org.entrypulse.entrypulse.model.EventTable;
import org.entrypulse.entrypulse.model.Guest;
import org.entrypulse.entrypulse.repository.EventGuestRepository;
import org.entrypulse.entrypulse.repository.EventRepository;
import org.entrypulse.entrypulse.repository.EventTableRepository;
import org.entrypulse.entrypulse.repository.GuestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventGuestService {

    private final EventGuestRepository eventGuestRepository;
    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;
    private final EventTableRepository eventTableRepository;

    @Transactional
    public EventGuestResponse createEventGuest(
            CreateEventGuestRequest request) {

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        Guest guest = guestRepository.findById(request.guestId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guest not found"));

        if (eventGuestRepository.existsByEvent_EventIdAndGuest_Id(
                request.eventId(), request.guestId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guest is already assigned to this event");
        }

        EventGuest eventGuest = new EventGuest();
        eventGuest.setEvent(event);
        eventGuest.setGuest(guest);
        eventGuest.setSeatNumber(request.seatNumber());

        if (request.eventTableId() != null
                && !request.eventTableId().isBlank()) {

            EventTable table = eventTableRepository
                    .findById(request.eventTableId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Event table not found"));

            if (table.getEvent() == null
                    || !table.getEvent().getEventId()
                    .equals(event.getEventId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Table does not belong to this event");
            }

            if (eventGuestRepository.countByEventTable_Id(table.getId())
                    >= table.getCapacity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Event table has reached its capacity");
            }

            eventGuest.setEventTable(table);
        }

        try {
            EventGuest saved = eventGuestRepository.saveAndFlush(eventGuest);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This guest is already assigned to this event",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<EventGuestResponse> getGuestsForEvent(String eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Event not found");
        }

        return eventGuestRepository.findByEventEventId(eventId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventGuestResponse getEventGuestById(String id) {
        EventGuest eventGuest = eventGuestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event guest assignment not found"));

        return toResponse(eventGuest);
    }

    @Transactional
    public EventGuestResponse updateEventGuest(
            String id,
            UpdateEventGuestRequest request) {

        EventGuest assignment = eventGuestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event guest assignment not found"));

        // Update the seat number.
        assignment.setSeatNumber(request.seatNumber());

        // Allow the table assignment to be changed or removed.
        if (request.eventTableId() == null
                || request.eventTableId().isBlank()) {

            assignment.setEventTable(null);

        } else {
            EventTable table = eventTableRepository
                    .findById(request.eventTableId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Event table not found"));

            // Ensure the table belongs to the same event.
            if (table.getEvent() == null
                    || !table.getEvent().getEventId()
                    .equals(assignment.getEvent().getEventId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Table does not belong to this event");
            }

            // Only check capacity when moving to a different table.
            boolean changingTable =
                    assignment.getEventTable() == null
                            || !assignment.getEventTable().getId()
                            .equals(table.getId());

            if (changingTable
                    && eventGuestRepository.countByEventTable_Id(table.getId())
                    >= table.getCapacity()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Event table has reached its capacity");
            }

            assignment.setEventTable(table);
        }

        EventGuest updated = eventGuestRepository.save(assignment);

        return toResponse(updated);
    }

    @Transactional
    public void deleteEventGuest(String id) {
        EventGuest eventGuest = eventGuestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event guest assignment not found"));

        eventGuestRepository.delete(eventGuest);
    }

    private EventGuestResponse toResponse(EventGuest assignment) {
        return new EventGuestResponse(
                assignment.getEventGuestId(),
                assignment.getEvent().getEventId(),
                assignment.getGuest().getId(),
                assignment.getGuest().getName(),
                assignment.getEventTable() == null
                        ? null : assignment.getEventTable().getId(),
                assignment.getSeatNumber()
        );
    }
}
