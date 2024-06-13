package ru.practicum.adminapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.StateAdminAction;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminEventServiceImpl implements AdminEventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final EventUtilService eventUtilService;
    private final Sort sort = Sort.by("eventDate").ascending();

    @Autowired
    public AdminEventServiceImpl(EventRepository eventRepository, EventMapper eventMapper,
                                 CategoryRepository categoryRepository,
                                 LocationRepository locationRepository,
                                 EventUtilService eventUtilService) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.eventUtilService = eventUtilService;
    }

    @Override
    public List<EventFullDto> getAll(EventQueryParams eventQueryParams) {
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

        EventFullDto result = getEventFullDto(existentEvent, updateRequest);
        Map<Long, Integer> hitsById = eventUtilService.getHitsByEvent(List.of(eventId));
        Map<Long, Long> confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(List.of(eventId));
        result.setViews(hitsById.getOrDefault(eventId, 0));
        result.setConfirmedRequests(confirmedRequestCount.getOrDefault(eventId, 0L));

        return result;
    }

    private List<EventFullDto> composeEventFullDtos(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        List<EventFullDto> result = events.stream()
                .map(eventMapper::convertEventToFullDto)
                .collect(Collectors.toList());
        if (!result.isEmpty()) {
            Map<Long, Integer> hitsById = eventUtilService.getHitsByEvent(eventIds);
            Map<Long, Long> confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(eventIds);

            for (EventFullDto el : result) {
                el.setViews(hitsById.getOrDefault(el.getId(), 0));
                el.setConfirmedRequests(confirmedRequestCount.getOrDefault(el.getId(), 0L));
            }
        }
        return result;
    }

    private EventFullDto getEventFullDto(Event event, UpdateEventAdminRequest updateRequest) {
        StateAdminAction stateAction = updateRequest.getStateAction();
        EventState eventState = event.getState();

        Event.EventBuilder copyOfExistentEvent = event.toBuilder();

        if (StateAdminAction.PUBLISH_EVENT.equals(stateAction)) {
            if (!EventState.PENDING.equals(eventState)) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + eventState);
            } else {
                copyOfExistentEvent.state(EventState.PUBLISHED);
                copyOfExistentEvent.publishedOn(LocalDateTime.now());
            }
        }

        if (StateAdminAction.REJECT_EVENT.equals(stateAction)) {
            if (EventState.CANCELED.equals(eventState) || EventState.PUBLISHED.equals(eventState)) {
                throw new ConflictException("Cannot cancel the event because it's not in the right state: "
                        + eventState);
            } else {
                copyOfExistentEvent.state(EventState.CANCELED);
                copyOfExistentEvent.publishedOn(null);

            }
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id="
                            + updateRequest.getCategory() + " was not found"));
            copyOfExistentEvent.category(category);
        }
        if (updateRequest.getLocation() != null) {
            Location location = Location.builder()
                    .lat(updateRequest.getLocation().getLat())
                    .lon(updateRequest.getLocation().getLat())
                    .build();
            location = locationRepository.save(location);
            copyOfExistentEvent.location(location);
        }
        if (updateRequest.getAnnotation() != null) {
            copyOfExistentEvent.annotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            copyOfExistentEvent.description(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            copyOfExistentEvent.eventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getPaid() != null) {
            copyOfExistentEvent.paid(updateRequest.getPaid());
        }
        if (updateRequest.getRequestModeration() != null) {
            copyOfExistentEvent.requestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            copyOfExistentEvent.title(updateRequest.getTitle());
        }
        if (updateRequest.getParticipantLimit() != null) {
            copyOfExistentEvent.participantLimit(updateRequest.getParticipantLimit());
        }

        Event updatedEvent = eventRepository.save(copyOfExistentEvent.build());

        return eventMapper.convertEventToFullDto(updatedEvent);
    }
}
