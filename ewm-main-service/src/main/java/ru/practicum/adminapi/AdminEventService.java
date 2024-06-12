package ru.practicum.adminapi;

import ru.practicum.event.EventQueryParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> getAll(EventQueryParams eventQueryParams);

    EventFullDto patch(long eventId, UpdateEventAdminRequest updateRequest);
}
