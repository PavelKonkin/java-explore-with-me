package ru.practicum.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.client.StatClient;
import ru.practicum.event.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestMapper;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventUtilService eventUtilService;
    private final EventMapper eventMapper;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;
    private final StatClient statClient;
    private final EventUserRatingRepository eventUserRatingRepository;


    private final Sort sort = Sort.by("eventDate").ascending();
    private static final String APP_NAME = "ewm-main-service";



    @Autowired
    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository,
                            EventUtilService eventUtilService,
                            EventMapper eventMapper,
                            LocationRepository locationRepository,
                            CategoryRepository categoryRepository,
                            ParticipationRequestRepository requestRepository,
                            ParticipationRequestMapper requestMapper,
                            StatClient statClient,
                            EventUserRatingRepository eventUserRatingRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventUtilService = eventUtilService;
        this.eventMapper = eventMapper;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
        this.statClient = statClient;
        this.eventUserRatingRepository = eventUserRatingRepository;
    }

    @Override
    public List<EventShortDto> getAll(long userId, Pageable page) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);

        if (events.isEmpty()) {
            return List.of();
        } else {

            return composeEventShortDtos(events);
        }
    }

    @Override
    public EventFullDto add(long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
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

        return eventMapper.convertEventToFullDto(event, 0, 0L, 0L, 0L);
    }

    @Override
    public EventFullDto get(long userId, long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId
                        + " and initiator id=" + userId + " was not found"));

        return getEventFullDto(event);
    }

    @Override
    public EventFullDto update(long userId, long eventId, UpdateEventUserRequest userRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId
                        + " and initiator id=" + userId + " was not found"));

        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        return getUpdatedEventFullDto(event, userRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(long userId, long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId
                        + " and initiator id=" + userId + " was not found"));

        List<ParticipationRequest> requests = requestRepository
                .findAllByEventIdAndEventInitiatorId(eventId, userId);

        return requests.stream()
                .map(requestMapper::convertParticipationRequest)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateRequests(long userId, long eventId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId
                        + " and initiator id=" + userId + " was not found"));
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        if (requests.stream().anyMatch(request -> request.getStatus() != ParticipationRequestStatus.PENDING)) {
            throw new ConflictException("Not all requests are in PENDING status");
        }

        EventRequestStatusUpdateResult.EventRequestStatusUpdateResultBuilder result
                = EventRequestStatusUpdateResult.builder();
        if (!requests.isEmpty()) {
            if (RequestUpdateAction.REJECTED.equals(updateRequest.getStatus())) { // отклоняем все
                result.rejectedRequests(updateRequestsStatus(requests, ParticipationRequestStatus.REJECTED));
            } else { // принимаем или отклоняем если достигнут лимит участников
                List<ParticipationRequest> confirmedRequests = requestRepository
                        .findAllByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
                int confirmedRequestCount = confirmedRequests.size();
                int participantLimit = event.getParticipantLimit();

                if (participantLimit != 0 && confirmedRequestCount >= participantLimit) { // лимит достигнут - отклоняем
                    throw new ConflictException("Event participation limit has reached");
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
                                ParticipationRequestStatus.REJECTED));
                    }
                }
            }
        }
        return result.build();
    }

    @Override
    public List<EventFullDto> getAll(AdminEventParams eventQueryParams) {
        Specification<Event> spec = Specification
                .where(EventSpecification
                        .hasEventDateBetween(eventQueryParams.getRangeStart(), eventQueryParams.getRangeEnd()))
                .and(EventSpecification.hasStateInList(eventQueryParams.getStates()))
                .and(EventSpecification.hasInitiatorIdInList(eventQueryParams.getUsers()))
                .and(EventSpecification.hasCategoryIdInList(eventQueryParams.getCategories()));
        Pageable page = new OffsetPage(eventQueryParams.getFrom(), eventQueryParams.getSize(), sort);

        Page<Event> eventPage = eventRepository.findAll(spec, page);
        List<Event> events = eventPage.getContent();

        return composeEventFullDtos(events);
    }

    @Override
    public EventFullDto patch(long eventId, UpdateEventAdminRequest updateRequest) {
        Event existentEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Event updatedEvent = getEventFullDto(existentEvent, updateRequest);
        updatedEvent = eventRepository.save(updatedEvent);

        Long eventRating = eventUserRatingRepository.findEventRatingByEventId(eventId);
        Long userRating = eventUserRatingRepository.findUserRatingByUserId(updatedEvent.getInitiator().getId());

        return eventMapper.convertEventToFullDto(updatedEvent, 0, 0L,
                eventRating, userRating);
    }

    @Override
    public List<EventShortDto> getAll(PublicEventParams params, String ipAddress, String uri) {
        Specification<Event> spec = Specification
                .where(EventSpecification.containsTextInAnnotationOrDescription(params.getText()))
                .and(EventSpecification
                        .hasEventDateBetween(params.getRangeStart(), params.getRangeEnd()))
                .and(EventSpecification.hasPublishedState())
                .and(EventSpecification.hasCategoryIdInList(params.getCategories()))
                .and(EventSpecification.isPaid(params.getPaid()))
                .and(EventSpecification.hasConfirmedRequestsLessThanLimit(params.getOnlyAvailable()));

        statClient.hits(APP_NAME, uri, ipAddress, LocalDateTime.now());

        List<Event> events;
        EventSort requestSort = EventSort.valueOf(params.getSort());

        if (params.getSort().equals(EventSort.EVENT_DATE.name())) {
            Pageable page = new OffsetPage(params.getFrom(), params.getSize(), sort);
            Page<Event> eventPage = eventRepository.findAll(spec, page);
            events = eventPage.getContent();
            return composeEventShortDtos(events);
        } else {
            events = eventRepository.findAll(spec);
            List<EventShortDto> result = composeEventShortDtos(events);
            if (result.isEmpty()) {
                return result;
            }
            int toIndex = Math.min(params.getFrom() + params.getSize(), events.size());
            if (params.getFrom() > toIndex) {
                return new ArrayList<>();
            }
            Comparator<EventShortDto> comparator;
            if (requestSort.equals(EventSort.VIEWS)) {
                comparator = Comparator.comparingInt(EventShortDto::getViews).reversed();
            } else {
                comparator = Comparator.comparingLong(EventShortDto::getRating).reversed();
            }
            result.sort(comparator);
            return result.subList(params.getFrom(), toIndex);
        }
    }

    @Override
    public EventFullDto get(long eventId, String remoteAddr, String requestURI) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        statClient.hits(APP_NAME, requestURI, remoteAddr, LocalDateTime.now());

        Map<Long, Integer> hits = eventUtilService.getHitsByEvent(List.of(eventId));
        Map<Long, Long> confirmedRequests = eventUtilService.getConfirmedRequestCountById(List.of(eventId));

        Long eventRating = eventUserRatingRepository.findEventRatingByEventId(eventId);
        Long userRating = eventUserRatingRepository.findUserRatingByUserId(event.getInitiator().getId());

        return eventMapper.convertEventToFullDto(event,
                hits.getOrDefault(eventId, 0),
                confirmedRequests.getOrDefault(eventId, 0L),
                eventRating, userRating);
    }

    @Override
    public EventFullDto like(long userId, long eventId) {
        return rateEvent(userId, eventId, EventRateAction.LIKE);
    }

    @Override
    public EventFullDto dislike(long userId, long eventId) {
        return rateEvent(userId, eventId, EventRateAction.DISLIKE);
    }

    @Override
    public EventFullDto removeLike(long userId, long eventId) {
        return rateEvent(userId, eventId, EventRateAction.REMOVE_LIKE);
    }

    @Override
    public EventFullDto removeDislike(long userId, long eventId) {
        return rateEvent(userId, eventId, EventRateAction.REMOVE_DISLIKE);
    }

    private EventFullDto rateEvent(long userId, long eventId, EventRateAction action) {
        Event event = eventRepository.findByIdWithGraph(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        if (event.getInitiator().equals(user)) {
            throw new ConflictException("Initiator may not rate his own event");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Not published events cannot be rated");
        }
        EventUserRating eventUserRating = eventUserRatingRepository.findByEventAndUser(event, user)
                .orElse(new EventUserRating(null, event, user, null));
        Boolean liked = eventUserRating.getLiked();
        switch (action) {
            case LIKE:
                if (liked == null || !liked) {
                    eventUserRating.setLiked(true);
                    eventUserRatingRepository.save(eventUserRating);
                }
                break;
            case DISLIKE:
                if (liked == null || liked) {
                    eventUserRating.setLiked(false);
                    eventUserRatingRepository.save(eventUserRating);
                }
                break;
            case REMOVE_LIKE:
                if (liked != null && liked) {
                    eventUserRatingRepository.delete(eventUserRating);
                }
                break;
            case REMOVE_DISLIKE:
                if (liked != null && !liked) {
                    eventUserRatingRepository.delete(eventUserRating);
                }
                break;
            default:
        }

        return getEventFullDto(event);
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

        Long eventRating = eventUserRatingRepository.findEventRatingByEventId(updatedEvent.getId());
        Long userRating = eventUserRatingRepository.findUserRatingByUserId(updatedEvent.getInitiator().getId());

        return eventMapper.convertEventToFullDto(updatedEvent, 0, 0L,
                eventRating, userRating);
    }

    private EventFullDto getEventFullDto(Event event) {
        long eventId = event.getId();
        Event copyOfEvent = event.toBuilder().build();

        Map<Long, Integer> hitByEvent = eventUtilService.getHitsByEvent(List.of(eventId));
        Map<Long, Long> confirmedRequest = eventUtilService.getConfirmedRequestCountById(List.of(eventId));

        int views = hitByEvent.getOrDefault(eventId, 0);
        long confirmedRequestsCount = confirmedRequest.getOrDefault(eventId, 0L);

        Long eventRating = eventUserRatingRepository.findEventRatingByEventId(eventId);
        Long userRating = eventUserRatingRepository.findUserRatingByUserId(copyOfEvent.getInitiator().getId());

        return eventMapper.convertEventToFullDto(copyOfEvent, views, confirmedRequestsCount, eventRating,
                userRating);
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

    private Event getEventFullDto(Event event, UpdateEventAdminRequest updateRequest) {
        StateAdminAction stateAction = updateRequest.getStateAction();
        EventState eventState = event.getState();

        Event.EventBuilder eventBuilder = event.toBuilder();

        if (StateAdminAction.PUBLISH_EVENT.equals(stateAction)) {
            if (!EventState.PENDING.equals(eventState)) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + eventState);
            } else {
                eventBuilder.state(EventState.PUBLISHED);
                eventBuilder.publishedOn(LocalDateTime.now());
            }
        }

        if (StateAdminAction.REJECT_EVENT.equals(stateAction)) {
            if (EventState.CANCELED.equals(eventState) || EventState.PUBLISHED.equals(eventState)) {
                throw new ConflictException("Cannot cancel the event because it's not in the right state: "
                        + eventState);
            } else {
                eventBuilder.state(EventState.CANCELED);
                eventBuilder.publishedOn(null);

            }
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id="
                            + updateRequest.getCategory() + " was not found"));
            eventBuilder.category(category);
        }
        if (updateRequest.getLocation() != null) {
            Location location = Location.builder()
                    .lat(updateRequest.getLocation().getLat())
                    .lon(updateRequest.getLocation().getLat())
                    .build();
            location = locationRepository.save(location);
            eventBuilder.location(location);
        }
        if (updateRequest.getAnnotation() != null) {
            eventBuilder.annotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            eventBuilder.description(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            eventBuilder.eventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getPaid() != null) {
            eventBuilder.paid(updateRequest.getPaid());
        }
        if (updateRequest.getRequestModeration() != null) {
            eventBuilder.requestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            eventBuilder.title(updateRequest.getTitle());
        }
        if (updateRequest.getParticipantLimit() != null) {
            eventBuilder.participantLimit(updateRequest.getParticipantLimit());
        }


        return eventBuilder.build();
    }

    private List<EventFullDto> composeEventFullDtos(List<Event> events) {

        Map<String, Map<Long, ?>> data = getEventData(events);


        return events.stream()
                .map(event -> eventMapper.convertEventToFullDto(event,
                        ((Map<Long, Integer>) data.get("hits")).getOrDefault(event.getId(), 0),
                        ((Map<Long, Long>) data.get("confirmedRequestCount")).getOrDefault(event.getId(), 0L),
                        ((Map<Long, Long>) data.get("eventsRating")).getOrDefault(event.getId(), 0L),
                        ((Map<Long, Long>) data.get("usersRating")).getOrDefault(event.getInitiator().getId(), 0L)))
                .collect(Collectors.toList());
    }

    private List<EventShortDto> composeEventShortDtos(List<Event> events) {

        Map<String, Map<Long, ?>> data = getEventData(events);
        return events.stream()
                .map(event -> eventMapper.convertEventToShortDto(event,
                        ((Map<Long, Integer>) data.get("hits")).getOrDefault(event.getId(), 0),
                        ((Map<Long, Long>) data.get("confirmedRequestCount")).getOrDefault(event.getId(), 0L),
                        ((Map<Long, Long>) data.get("eventsRating")).getOrDefault(event.getId(), 0L),
                        ((Map<Long, Long>) data.get("usersRating")).getOrDefault(event.getInitiator().getId(), 0L)))
                .collect(Collectors.toList());
    }

    private Map<String, Map<Long, ?>> getEventData(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> hitsById;
        Map<Long, Long> confirmedRequestCount;

        if (!eventIds.isEmpty()) {
            hitsById = eventUtilService.getHitsByEvent(eventIds);
            confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(eventIds);
        } else {
            hitsById = new HashMap<>();
            confirmedRequestCount = new HashMap<>();
        }

        Map<Long, Long> usersRating = eventUserRatingRepository.findUsersRatingByUserIds(events.stream()
                .map(el -> el.getInitiator().getId())
                .distinct()
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(
                        RatingDto::getId,  // eventId
                        RatingDto::getRating    // rating
                ));
        Map<Long, Long> eventsRating = eventUserRatingRepository.findRatingOfEventsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        RatingDto::getId,  // eventId
                        RatingDto::getRating    // rating
                ));

        Map<String, Map<Long, ?>> result = new HashMap<>();
        result.put("hits", hitsById);
        result.put("confirmedRequestCount", confirmedRequestCount);
        result.put("usersRating", usersRating);
        result.put("eventsRating", eventsRating);
        return result;
    }
}
