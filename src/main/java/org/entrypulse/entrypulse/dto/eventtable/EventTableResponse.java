package org.entrypulse.entrypulse.dto.eventtable;

import org.entrypulse.entrypulse.model.EventTable;

public record EventTableResponse(String tableId, int tableNumber, int capacity) {
    public static EventTableResponse from(EventTable t) {
        return new EventTableResponse(t.getId(), t.getTableNumber(), t.getCapacity());
    }
}