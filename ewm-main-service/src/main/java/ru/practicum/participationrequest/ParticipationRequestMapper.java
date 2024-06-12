package ru.practicum.participationrequest;

import org.springframework.stereotype.Component;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

@Component
public class ParticipationRequestMapper {
    public ParticipationRequestDto convertParticipationRequest(ParticipationRequest participationRequest) {
        return ParticipationRequestDto.builder()
                .id(participationRequest.getId())
                .requesterId(participationRequest.getRequester().getId())
                .eventId(participationRequest.getEvent().getId())
                .status(participationRequest.getStatus().name())
                .created(participationRequest.getCreated())
                .build();
    }
}
