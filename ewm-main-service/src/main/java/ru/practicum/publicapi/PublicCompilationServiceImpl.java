package ru.practicum.publicapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationMapper;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.eventcompilation.EventCompilation;
import ru.practicum.eventcompilation.EventCompilationRepository;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PublicCompilationServiceImpl implements PublicCompilationService {
    private final CompilationRepository compilationRepository;
    private final EventCompilationRepository eventCompilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;

    @Autowired
    public PublicCompilationServiceImpl(CompilationRepository compilationRepository,
                                        EventCompilationRepository eventCompilationRepository,
                                        CompilationMapper compilationMapper,
                                        EventMapper eventMapper,
                                        EventUtilService eventUtilService) {
        this.compilationRepository = compilationRepository;
        this.eventCompilationRepository = eventCompilationRepository;
        this.compilationMapper = compilationMapper;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
    }

    @Override
    public List<CompilationDto> getAll(boolean pinned, Pageable page) {
        List<Compilation> compilations = compilationRepository.findAllByPinned(pinned, page);
        List<CompilationDto> result = compilations.stream()
                .map(compilationMapper::convertCompilation)
                .collect(Collectors.toList());

        if (!result.isEmpty()) {
            List<Long> compilationIds = compilations.stream()
                    .map(Compilation::getId)
                    .collect(Collectors.toList());
            List<EventCompilation> eventCompilations = eventCompilationRepository
                    .findAllByCompilationIdIn(compilationIds);
            List<Long> eventIds = eventCompilations.stream()
                    .map(entry -> entry.getEvent().getId())
                    .collect(Collectors.toList());

            if (!eventIds.isEmpty()) {
                Map<Long, Integer> hitByEventId = eventUtilService.getHitsByEvent(eventIds);

                Map<Long, Long> confirmedRequestCountByEventId = eventUtilService
                        .getConfirmedRequestCountById(eventIds);

                for (CompilationDto compilationDto : result) {
                    List<Event> eventsOfCompilation = eventCompilations.stream()
                            .filter(entry -> Objects.equals(entry.getCompilation().getId(), compilationDto.getId()))
                            .map(EventCompilation::getEvent)
                            .collect(Collectors.toList());
                    List<EventShortDto> eventShortDtos = createEventShortDtos(eventsOfCompilation,
                            hitByEventId, confirmedRequestCountByEventId);
                    compilationDto.setEvents(eventShortDtos);
                }
            }
        }

        return result;
    }

    @Override
    public CompilationDto get(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        CompilationDto result = compilationMapper.convertCompilation(compilation);

        List<EventCompilation> eventCompilations = eventCompilationRepository
                .findAllByCompilationId(compId);
        if (!eventCompilations.isEmpty()) {
            List<Long> eventIds = eventCompilations.stream()
                    .map(entry -> entry.getEvent().getId())
                    .collect(Collectors.toList());
            List<Event> events = eventCompilations.stream()
                    .map(EventCompilation::getEvent)
                    .collect(Collectors.toList());

            Map<Long, Integer> hitByEventId = eventUtilService.getHitsByEvent(eventIds);

            Map<Long, Long> confirmedRequestCountByEventId = eventUtilService.getConfirmedRequestCountById(eventIds);

            List<EventShortDto> eventShortDtos = createEventShortDtos(events,
                    hitByEventId, confirmedRequestCountByEventId);

            result.setEvents(eventShortDtos);
        }

        return result;
    }

    private List<EventShortDto> createEventShortDtos(List<Event> events, Map<Long, Integer> hitsByEvent,
                                                     Map<Long, Long> confirmedRequestCountById) {
        List<EventShortDto> eventShortDtos = new ArrayList<>();
        for (Event event : events) {
            EventShortDto eventShortDto = eventMapper.convertEventToShortDto(event);
            eventShortDto.setViews(hitsByEvent.getOrDefault(eventShortDto.getId(), 0));
            eventShortDto.setConfirmedRequests(confirmedRequestCountById
                    .getOrDefault(eventShortDto.getId(), 0L));
            eventShortDtos.add(eventShortDto);
        }
        return eventShortDtos;
    }
}
