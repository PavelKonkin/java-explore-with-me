package ru.practicum.publicapi;

import ru.practicum.event.EventQueryParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

public interface PublicEventService {
    List<EventShortDto> getAll(EventQueryParams eventQueryParams, String ipAddress, String uri);

    EventFullDto get(long eventId, String remoteAddr, String requestURI);
}
