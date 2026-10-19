
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.CheckIn;
import org.entrypulse.entrypulse.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    public record CheckInRequest(String token) {}

    @PostMapping
    public ResponseEntity<CheckIn> checkInGuest(
            @RequestBody CheckInRequest request,
            Authentication authentication) {

        CheckIn checkIn = checkInService.checkInGuest(
                request.token(),
                authentication.getName());

        return ResponseEntity.ok(checkIn);
    }
}
