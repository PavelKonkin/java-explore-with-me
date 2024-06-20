package ru.practicum.adminapi;

import org.springframework.beans.factory.annotation.Qualifier;
import ru.practicum.event.AdminEventParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;

import java.util.List;

@Qualifier(value = "admin")
public interface EventService {
    List<EventFullDto> getAll(AdminEventParams eventQueryParams);

    EventFullDto patch(long eventId, UpdateEventAdminRequest updateRequest);
}
