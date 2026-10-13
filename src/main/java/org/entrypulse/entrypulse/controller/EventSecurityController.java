package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.EventSecurity;
import org.entrypulse.entrypulse.service.EventSecurityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event-security")
@RequiredArgsConstructor
public class EventSecurityController {
    private final EventSecurityService eventSecurityService;

    @PostMapping("/assign")
    public EventSecurity assignSecurityToEvent(
            @RequestParam String eventId,
            @RequestParam String userId
    ) {
        return eventSecurityService.assignSecurityToEvent(eventId, userId);
    }

    @GetMapping("/event/{eventId}")
    public List<EventSecurity> getSecurityForEvent(@PathVariable String eventId) {
        return eventSecurityService.getSecurityForEvent(eventId);
    }

    @GetMapping("/user/{userId}")
    public List<EventSecurity> getEventsForSecurityUser(@PathVariable String userId) {
        return eventSecurityService.getEventsForSecurityUser(userId);
    }
}
