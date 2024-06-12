package ru.practicum.privateapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestMapper;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivateParticipationRequestServiceImpl implements PrivateParticipationRequestService {
    private final ParticipationRequestRepository participationRequestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper participationRequestMapper;

    @Autowired
    public  PrivateParticipationRequestServiceImpl(ParticipationRequestRepository participationRequestRepository,
                                                   UserRepository userRepository, EventRepository eventRepository,
                                                   ParticipationRequestMapper participationRequestMapper) {
        this.participationRequestRepository = participationRequestRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.participationRequestMapper = participationRequestMapper;
    }

    @Override
    public ParticipationRequestDto add(long requesterId, long eventId, LocalDateTime created) {
        User requester = getUser(requesterId);
        Event event = getEvent(eventId);

        checkRequest(requester, event);

        ParticipationRequestStatus requestStatus;
        if (event.isRequestModeration()) {
            requestStatus = ParticipationRequestStatus.PENDING;
        } else {
            requestStatus = ParticipationRequestStatus.CONFIRMED;
        }

        ParticipationRequest participationRequest = ParticipationRequest.builder()
                .requester(requester)
                .event(event)
                .created(created)
                .status(requestStatus)
                .build();

        return participationRequestMapper
                .convertParticipationRequest(participationRequestRepository.save(participationRequest));
    }

    @Override
    public List<ParticipationRequestDto> get(long requesterId) {
        User requester = getUser(requesterId);
        List<ParticipationRequest> requestList = participationRequestRepository.findAllByRequesterId(requesterId);
        return requestList.stream()
                .map(participationRequestMapper::convertParticipationRequest)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancel(long requesterId, long requestId) {
        User requester = getUser(requesterId);
        ParticipationRequest participationRequest = getRequest(requesterId, requestId);

        ParticipationRequest canceledRequest = participationRequest.toBuilder()
                .status(ParticipationRequestStatus.CANCELED)
                .build();

        return participationRequestMapper
                .convertParticipationRequest(participationRequestRepository.save(canceledRequest));
    }

    private void checkRequest(User requester, Event event) {
        if (event.getInitiator().equals(requester)) {
            throw new ConflictException("Initiator of event cannot request participation in it");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Attempt of request participation in not published event");
        }

        int limitOfParticipants = event.getParticipantLimit();
        if (limitOfParticipants != 0) {
            List<ParticipationRequest> eventParticipation
                    = participationRequestRepository.findAllByEventId(event.getId());
            if (limitOfParticipants <= eventParticipation.size()) {
                throw new ConflictException("Limit of participants of event is exceeded");
            }
        }
    }

    private User getUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event getEvent(long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private ParticipationRequest getRequest(long requesterId, long requestId) {
        return participationRequestRepository.findByIdAndRequesterId(requestId, requesterId)
                .orElseThrow(() ->
                        new NotFoundException("Request with id=" + requestId + " was not found"));
    }
}
