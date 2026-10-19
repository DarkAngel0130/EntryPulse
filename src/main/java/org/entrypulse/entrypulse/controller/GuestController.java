
package org.entrypulse.entrypulse.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.model.Guest;
import org.entrypulse.entrypulse.service.GuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    // Create a guest
    @PostMapping
    public ResponseEntity<Guest> createGuest(
            @Valid @RequestBody Guest guest) {

        Guest createdGuest = guestService.createGuest(guest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdGuest);
    }

    // Retrieve all guests
    @GetMapping
    public ResponseEntity<List<Guest>> getAllGuests() {

        return ResponseEntity.ok(
                guestService.getAllGuests()
        );
    }

    // Retrieve a guest by ID
    @GetMapping("/{id}")
    public ResponseEntity<Guest> getGuestById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                guestService.getGuestById(id)
        );
    }

    // Update a guest
    @PutMapping("/{id}")
    public ResponseEntity<Guest> updateGuest(
            @PathVariable String id,
            @Valid @RequestBody Guest guest) {

        return ResponseEntity.ok(
                guestService.updateGuest(id, guest)
        );
    }

    // Delete a guest
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(
            @PathVariable String id) {

        guestService.deleteGuest(id);

        return ResponseEntity.noContent().build();
    }
}
