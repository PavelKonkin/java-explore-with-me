package ru.practicum.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> getAll(long userId, Pageable page);

    EventFullDto add(long userId, NewEventDto newEventDto);

    EventFullDto get(long userId, long eventId);

    EventFullDto update(long userId, long eventId, UpdateEventUserRequest userRequest);

    List<ParticipationRequestDto> getRequests(long userId, long eventId);

    EventRequestStatusUpdateResult updateRequests(long userId, long eventId,
                                                  EventRequestStatusUpdateRequest updateRequest);

    List<EventFullDto> getAll(AdminEventParams eventQueryParams);

    EventFullDto patch(long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getAll(PublicEventParams params, String ipAddress, String uri);

    EventFullDto get(long eventId, String remoteAddr, String requestURI);

    EventFullDto like(long userId, long eventId);

    EventFullDto dislike(long userId, long eventId);

    EventFullDto removeLike(long userId, long eventId);

    EventFullDto removeDislike(long userId, long eventId);
}
