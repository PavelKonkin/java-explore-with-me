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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service(value = "adminEventService")
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final EventUtilService eventUtilService;
    private final Sort sort = Sort.by("eventDate").ascending();

    @Autowired
    public EventServiceImpl(EventRepository eventRepository, EventMapper eventMapper,
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

        return eventMapper.convertEventToFullDto(updatedEvent, 0, 0L);
    }

    private List<EventFullDto> composeEventFullDtos(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> hitsById;
        Map<Long, Long> confirmedRequestCount;
        if (!eventIds.isEmpty()) {
            hitsById = eventUtilService.getHitsByEvent(eventIds);
            confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(eventIds);
        } else {
            confirmedRequestCount = new HashMap<>();
            hitsById = new HashMap<>();
        }

        return events.stream()
                .map(event -> eventMapper.convertEventToFullDto(event,
                        hitsById.getOrDefault(event.getId(), 0),
                        confirmedRequestCount.getOrDefault(event.getId(), 0L)))
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
}
