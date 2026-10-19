
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardService.DashboardSummary getDashboard(
            Authentication authentication) {

        return dashboardService.getOrganiserDashboard(
                authentication.getName());
    }

    @GetMapping("/events/{eventId}")
    public DashboardService.EventDashboardSummary getEventDashboard(
            @PathVariable String eventId,
            Authentication authentication) {

        return dashboardService.getEventDashboard(
                eventId,
                authentication.getName());
    }
}
