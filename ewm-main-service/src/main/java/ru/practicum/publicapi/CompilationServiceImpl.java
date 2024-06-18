package ru.practicum.publicapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationMapper;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service(value = "publicCompilationService")
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;

    @Autowired
    public CompilationServiceImpl(CompilationRepository compilationRepository,
                                  CompilationMapper compilationMapper,
                                  EventMapper eventMapper,
                                  EventUtilService eventUtilService) {
        this.compilationRepository = compilationRepository;
        this.compilationMapper = compilationMapper;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
    }

    @Override
    public List<CompilationDto> getAll(boolean pinned, Pageable page) {
        List<Compilation> compilations = compilationRepository.findAllByPinned(pinned, page);
        List<CompilationDto> result = new ArrayList<>();

        if (compilations.isEmpty()) {
            return result;
        } else {
            for (Compilation compilation : compilations) {
                Set<Event> eventsOfCompilation = compilation.getEvents();
                List<Long> eventIds = eventsOfCompilation.stream()
                        .map(Event::getId)
                        .collect(Collectors.toList());
                Map<Long, Integer> hitByEventId = new HashMap<>();
                Map<Long, Long> confirmedRequestCountByEventId = new HashMap<>();

                if (!eventIds.isEmpty()) {
                    hitByEventId = eventUtilService.getHitsByEvent(eventIds);
                    confirmedRequestCountByEventId = eventUtilService
                            .getConfirmedRequestCountById(eventIds);
                }

                List<EventShortDto> eventShortDtos = createEventShortDtos(eventsOfCompilation,
                        hitByEventId, confirmedRequestCountByEventId);
                CompilationDto compilationDto = compilationMapper.convertCompilation(compilation, eventShortDtos);
                result.add(compilationDto);
            }
        }
        return result;
    }

    @Override
    public CompilationDto get(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        List<EventShortDto> eventShortDtos;

        if (compilation.getEvents() == null || compilation.getEvents().isEmpty()) {
            eventShortDtos = List.of();
        } else {
            Set<Event> events = compilation.getEvents();
            List<Long> eventIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());

            Map<Long, Integer> hitByEventId = eventUtilService.getHitsByEvent(eventIds);

            Map<Long, Long> confirmedRequestCountByEventId = eventUtilService.getConfirmedRequestCountById(eventIds);

            eventShortDtos = createEventShortDtos(events,
                    hitByEventId, confirmedRequestCountByEventId);
        }

        return compilationMapper.convertCompilation(compilation, eventShortDtos);
    }

    private List<EventShortDto> createEventShortDtos(Set<Event> events, Map<Long, Integer> hitsByEvent,
                                                     Map<Long, Long> confirmedRequestCountById) {
        List<EventShortDto> eventShortDtos = new ArrayList<>();
        for (Event event : events) {
            int views = hitsByEvent.getOrDefault(event.getId(), 0);
            long confirmedRequests = confirmedRequestCountById
                    .getOrDefault(event.getId(), 0L);
            EventShortDto eventShortDto = eventMapper.convertEventToShortDto(event, views, confirmedRequests);
            eventShortDtos.add(eventShortDto);
        }
        return eventShortDtos;
    }
}
