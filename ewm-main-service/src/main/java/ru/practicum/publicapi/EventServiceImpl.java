package ru.practicum.publicapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service(value = "publicEventService")
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final StatClient statClient;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;
    private final Sort sort = Sort.by("eventDate").ascending();
    private static final String APP_NAME = "ewm-main-service";


    @Autowired
    public EventServiceImpl(EventRepository eventRepository, StatClient statClient,
                            EventMapper eventMapper,
                            EventUtilService eventUtilService) {
        this.eventRepository = eventRepository;
        this.statClient = statClient;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
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
            result.sort(Comparator.comparingInt(EventShortDto::getViews).reversed());
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

        return eventMapper.convertEventToFullDto(event,
                hits.getOrDefault(eventId, 0),
                confirmedRequests.getOrDefault(eventId, 0L));
    }

    private List<EventShortDto> composeEventShortDtos(List<Event> events) {
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
        return events.stream()
                .map(event -> eventMapper.convertEventToShortDto(event,
                        hitsById.getOrDefault(event.getId(), 0),
                        confirmedRequestCount.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }
}
