package ru.practicum.privateapi;

import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PrivateParticipationRequestService {
    ParticipationRequestDto add(long userId, long eventId, LocalDateTime created);

    List<ParticipationRequestDto> get(long userId);

    ParticipationRequestDto cancel(long userId, long requestId);
}
