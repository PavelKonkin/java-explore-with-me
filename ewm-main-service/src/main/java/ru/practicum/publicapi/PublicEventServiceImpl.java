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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PublicEventServiceImpl implements PublicEventService {
    private final EventRepository eventRepository;
    private final StatClient statClient;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;
    private final Sort sort = Sort.by("eventDate").ascending();
    private static final String APP_NAME = "ewm-main-service";


    @Autowired
    public PublicEventServiceImpl(EventRepository eventRepository, StatClient statClient,
                                  EventMapper eventMapper,
                                  EventUtilService eventUtilService) {
        this.eventRepository = eventRepository;
        this.statClient = statClient;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
    }

    @Override
    public List<EventShortDto> getAll(EventQueryParams eventQueryParams, String ipAddress, String uri) {
        Specification<Event> spec = Specification
                .where(EventSpecification.containsTextInAnnotationOrDescription(eventQueryParams.getText()))
                .and(EventSpecification
                        .hasEventDateBetween(eventQueryParams.getRangeStart(), eventQueryParams.getRangeEnd()))
                .and(EventSpecification.hasPublishedState())
                .and(EventSpecification.hasCategoryIdInList(eventQueryParams.getCategories()))
                .and(EventSpecification.isPaid(eventQueryParams.getPaid()))
                .and(EventSpecification.hasConfirmedRequestsLessThanLimit(eventQueryParams.getOnlyAvailable()));

        try {
            statClient.hits(APP_NAME, uri, ipAddress, LocalDateTime.now());
        } catch (Throwable ignore) {

        }

        List<Event> events;

        if (eventQueryParams.getSort().equals(EventSort.EVENT_DATE.name())) {
            Pageable page = new OffsetPage(eventQueryParams.getFrom(), eventQueryParams.getSize(), sort);
            Page<Event> eventPage = eventRepository.findAll(spec, page);
            events = eventPage.getContent();
            return composeEventShortDtos(events);
        } else {
            events = eventRepository.findAll(spec);
            List<EventShortDto> result = composeEventShortDtos(events);
            if (result.isEmpty()) {
                return result;
            }
            int toIndex = Math.min(eventQueryParams.getFrom() + eventQueryParams.getSize(), events.size());
            if (eventQueryParams.getFrom() > toIndex) {
                return new ArrayList<>();
            }
            result.sort(Comparator.comparingInt(EventShortDto::getViews).reversed());
            return result.subList(eventQueryParams.getFrom(), toIndex);
        }
    }

    @Override
    public EventFullDto get(long eventId, String remoteAddr, String requestURI) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new IllegalArgumentException("Event must be published");
        }

        EventFullDto result = eventMapper.convertEventToFullDto(event);

        try {
            statClient.hits(APP_NAME, requestURI, remoteAddr, LocalDateTime.now());
        } catch (Throwable ignore) {

        }

        Map<Long, Integer> hits = eventUtilService.getHitsByEvent(List.of(eventId));
        Map<Long, Long> confirmedRequests = eventUtilService.getConfirmedRequestCountById(List.of(eventId));

        result.setViews(hits.getOrDefault(eventId, 0));
        result.setConfirmedRequests(confirmedRequests.getOrDefault(eventId, 0L));

        return result;
    }

    private List<EventShortDto> composeEventShortDtos(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> hitsById = eventUtilService.getHitsByEvent(eventIds);
        Map<Long, Long> confirmedRequestCount = eventUtilService.getConfirmedRequestCountById(eventIds);
        List<EventShortDto> result = events.stream()
                .map(eventMapper::convertEventToShortDto)
                .collect(Collectors.toList());
        for (EventShortDto el : result) {
            el.setViews(hitsById.getOrDefault(el.getId(), 0));
            el.setConfirmedRequests(confirmedRequestCount.getOrDefault(el.getId(), 0L));
        }
        return result;
    }
}
