package org.entrypulse.entrypulse.dto.eventtable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEventTableRequest(
        @NotBlank String eventId,
        @Min(value = 1, message = "Table number must be at least 1")
        int tableNumber,
        @Min(value = 1, message = "Capacity must be at least 1")
        int capacity
) {}
