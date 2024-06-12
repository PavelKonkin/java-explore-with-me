package ru.practicum.privateapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
import ru.practicum.event.dto.*;
import ru.practicum.eventutil.EventUtilService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PrivateEventServiceImpl implements PrivateEventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventUtilService eventUtilService;
    private final EventMapper eventMapper;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;

    @Autowired
    public PrivateEventServiceImpl(EventRepository eventRepository,
                                   UserRepository userRepository,
                                   EventUtilService eventUtilService,
                                   EventMapper eventMapper,
                                   LocationRepository locationRepository,
                                   CategoryRepository categoryRepository,
                                   ParticipationRequestRepository requestRepository,
                                   ParticipationRequestMapper requestMapper) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventUtilService = eventUtilService;
        this.eventMapper = eventMapper;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
    }

    @Override
    public List<EventShortDto> getAll(long userId, Pageable page) {
        getUserIfExist(userId);

        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);

        if (!events.isEmpty()) {
            List<Long> eventIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());

            List<EventShortDto> result = events.stream()
                    .map(eventMapper::convertEventToShortDto)
                    .collect(Collectors.toList());

            Map<Long, Integer> hitsByEventId = eventUtilService.getHitsByEvent(eventIds);
            Map<Long, Long> confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(eventIds);

            for (EventShortDto dto : result) {
                dto.setViews(hitsByEventId.getOrDefault(dto.getId(), 0));
                dto.setConfirmedRequests(confirmedRequestCount.getOrDefault(dto.getId(), 0L));
            }

            return result;
        } else {
            return List.of();
        }
    }

    @Override
    public EventFullDto add(long userId, NewEventDto newEventDto) {
        User user = getUserIfExist(userId);
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id="
                        + newEventDto.getCategory() + " was not found"));
        Location location = Location.builder()
                .lon(newEventDto.getLocation().getLon())
                .lat(newEventDto.getLocation().getLat())
                .build();
        location = locationRepository.save(location);
        Event event = Event.builder()
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .location(location)
                .paid(newEventDto.isPaid())
                .title(newEventDto.getTitle())
                .createdOn(LocalDateTime.now())
                .description(newEventDto.getDescription())
                .annotation(newEventDto.getAnnotation())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.isRequestModeration())
                .eventDate(newEventDto.getEventDate())
                .build();

        event = eventRepository.save(event);

        return eventMapper.convertEventToFullDto(event);
    }

    @Override
    public EventFullDto get(long userId, long eventId) {
        getUserIfExist(userId);
        Event event = getEventIfExists(eventId, userId);

        EventFullDto result = eventMapper.convertEventToFullDto(event);

        return populateEventFullDtoWithViewsAndConfirmedRequests(result, eventId);
    }

    @Override
    public EventFullDto update(long userId, long eventId, UpdateEventUserRequest userRequest) {
        getUserIfExist(userId);
        Event event = getEventIfExists(eventId, userId);

        checkEventAvailabilityToUpdate(event, userRequest);

        EventFullDto result = getUpdatedEventFullDto(event, userRequest);

        return populateEventFullDtoWithViewsAndConfirmedRequests(result, eventId);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(long userId, long eventId) {
        getUserIfExist(userId);
        getEventIfExists(eventId, userId);

        List<ParticipationRequest> requests = requestRepository
                .findAllByEventIdAndEventInitiatorId(eventId, userId);

        return requests.stream()
                .map(requestMapper::convertParticipationRequest)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateRequests(long userId, long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        getUserIfExist(userId);
        Event event = getEventIfExists(eventId, userId);
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        if (requests.stream().anyMatch(request -> request.getStatus() != ParticipationRequestStatus.PENDING)) {
            throw new IllegalArgumentException("Not all requests are in PENDING status");
        }

        EventRequestStatusUpdateResult.EventRequestStatusUpdateResultBuilder result
                = EventRequestStatusUpdateResult.builder();
        if (!requests.isEmpty()) {
            if (RequestUpdateAction.REJECTED.equals(updateRequest.getStatus())) { // отклоняем все
                result.rejectedRequests(updateRequestsStatus(requests, ParticipationRequestStatus.CANCELED));
            } else { // принимаем или отклоняем если достигнут лимит участников
                List<ParticipationRequest> confirmedRequests = requestRepository
                        .findAllByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
                int confirmedRequestCount = confirmedRequests.size();
                int participantLimit = event.getParticipantLimit();

                if (participantLimit != 0 && confirmedRequestCount >= participantLimit) { // лимит достигнут - отклоняем
                    result.rejectedRequests(updateRequestsStatus(requests, ParticipationRequestStatus.CANCELED));
                } else if (participantLimit == 0) { // нет лимита - принимаем все
                    result.confirmedRequests(updateRequestsStatus(requests, ParticipationRequestStatus.CONFIRMED));
                } else { //часть заявок принимаем - до лимита, остальные отклоняем
                    int availableParticipation = participantLimit - confirmedRequestCount;
                    int lastRequestIndexToConfirm = Math.min(requests.size(), availableParticipation);
                    List<ParticipationRequest> requestToConfirm = requests.subList(0, lastRequestIndexToConfirm);
                    List<ParticipationRequest> requestToCancel = requests.subList(lastRequestIndexToConfirm,
                            requests.size());
                    result.confirmedRequests(updateRequestsStatus(requestToConfirm,
                            ParticipationRequestStatus.CONFIRMED));
                    if (!requestToCancel.isEmpty()) {
                        result.rejectedRequests(updateRequestsStatus(requestToCancel,
                                ParticipationRequestStatus.CANCELED));
                    }
                }
            }
        }
        return result.build();
    }

    private User getUserIfExist(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event getEventIfExists(long eventId, long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId
                        + " and initiator id=" + userId + " was not found"));
    }

    private void checkEventAvailabilityToUpdate(Event event, UpdateEventUserRequest userRequest) {
        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new IllegalArgumentException("Only pending or canceled events can be changed");
        }

        if (EventState.PENDING.equals(event.getState())
                && StateUserAction.SEND_TO_REVIEW.equals(userRequest.getStateAction())) {
            throw new IllegalArgumentException("Only canceled events can be sent to review");
        }

        if (EventState.CANCELED.equals(event.getState())
                && StateUserAction.CANCEL_REVIEW.equals(userRequest.getStateAction())) {
            throw new IllegalArgumentException("Only pending events can be canceled");
        }
    }

    private EventFullDto getUpdatedEventFullDto(Event event, UpdateEventUserRequest userRequest) {
        Event.EventBuilder resultEvent = event.toBuilder();
        if (userRequest.getRequestModeration() != null) {
            resultEvent.requestModeration(userRequest.getRequestModeration());
        }
        if (userRequest.getPaid() != null) {
            resultEvent.paid(userRequest.getPaid());
        }
        if (userRequest.getEventDate() != null) {
            resultEvent.eventDate(userRequest.getEventDate());
        }
        if (userRequest.getCategory() != null) {
            Category category = categoryRepository.findById(userRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id="
                            + userRequest.getCategory() + " was not found"));
            resultEvent.category(category);
        }
        if (userRequest.getAnnotation() != null) {
            resultEvent.annotation(userRequest.getAnnotation());
        }
        if (userRequest.getDescription() != null) {
            resultEvent.description(userRequest.getDescription());
        }
        if (userRequest.getTitle() != null) {
            resultEvent.title(userRequest.getTitle());
        }
        if (userRequest.getParticipantLimit() != null) {
            resultEvent.participantLimit(userRequest.getParticipantLimit());
        }
        if (userRequest.getLocation() != null) {
            Location location = Location.builder()
                    .lat(userRequest.getLocation().getLat())
                    .lon(userRequest.getLocation().getLon())
                    .build();
            location = locationRepository.save(location);
            resultEvent.location(location);
        }
        if (StateUserAction.CANCEL_REVIEW.equals(userRequest.getStateAction())) {
            resultEvent.state(EventState.CANCELED);
        } else if (StateUserAction.SEND_TO_REVIEW.equals(userRequest.getStateAction())) {
            resultEvent.state(EventState.PENDING);
        }

        Event updatedEvent = resultEvent.build();
        updatedEvent = eventRepository.save(updatedEvent);

        return eventMapper.convertEventToFullDto(updatedEvent);
    }

    private EventFullDto populateEventFullDtoWithViewsAndConfirmedRequests(EventFullDto eventFullDto, long eventId) {
        EventFullDto copyOfDto = eventFullDto.toBuilder().build();
        Map<Long, Integer> hitByEvent = eventUtilService.getHitsByEvent(List.of(eventId));
        Map<Long, Long> confirmedRequest = eventUtilService.getConfirmedRequestCountById(List.of(eventId));

        copyOfDto.setViews(hitByEvent.getOrDefault(eventId, 0));
        copyOfDto.setConfirmedRequests(confirmedRequest.getOrDefault(eventId, 0L));
        return copyOfDto;
    }

    private List<ParticipationRequestDto> updateRequestsStatus(List<ParticipationRequest> requests,
                                                                ParticipationRequestStatus newStatus) {
        requests = requests.stream()
                .peek(el -> el.setStatus(newStatus))
                .collect(Collectors.toList());

        requestRepository.saveAll(requests);

        return requests.stream()
                .map(requestMapper::convertParticipationRequest)
                .collect(Collectors.toList());
    }
}
