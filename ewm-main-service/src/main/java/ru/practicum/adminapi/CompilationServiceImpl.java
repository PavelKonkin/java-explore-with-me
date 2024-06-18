package ru.practicum.adminapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.compilation.Compilation;
import ru.practicum.compilation.CompilationMapper;
import ru.practicum.compilation.CompilationRepository;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;
import ru.practicum.event.EventRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service(value = "adminCompilationService")
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;

    @Autowired
    public CompilationServiceImpl(CompilationRepository compilationRepository,
                                  EventRepository eventRepository,
                                  CompilationMapper compilationMapper,
                                  EventMapper eventMapper,
                                  EventUtilService eventUtilService) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.compilationMapper = compilationMapper;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
    }

    @Override
    @Transactional
    public CompilationDto add(NewCompilationDto newCompilationDto) {
        Compilation compilation = createCompilation(newCompilationDto);
        List<Long> eventIds = newCompilationDto.getEvents();
        List<Event> events = findEvents(eventIds);
        if (!events.isEmpty()) {
            compilation.getEvents().addAll(events);
        }
        compilation = compilationRepository.save(compilation);
        List<EventShortDto> eventsShortDtos = createEventShortDtos(events, eventIds);

        return compilationMapper.convertCompilation(compilation, eventsShortDtos);
    }

    @Override
    public void delete(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        compilationRepository.delete(compilation);
    }

    @Override
    public CompilationDto patch(long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation existentCompilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        Compilation copyOfExistentCompilation = existentCompilation.toBuilder().build();
        if (updateCompilationRequest.getPinned() != null) {
            copyOfExistentCompilation.setPinned(updateCompilationRequest.getPinned());
        }
        if (updateCompilationRequest.getTitle() != null) {
            copyOfExistentCompilation.setTitle(updateCompilationRequest.getTitle());
        }

        List<Long> eventIds = new ArrayList<>();
        List<Event> events = new ArrayList<>();

        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            copyOfExistentCompilation.setEvents(new HashSet<>());
            eventIds = updateCompilationRequest.getEvents();
            events = findEvents(eventIds);
            copyOfExistentCompilation.getEvents().addAll(events);
        } else if (copyOfExistentCompilation.getEvents() != null && !copyOfExistentCompilation.getEvents().isEmpty()) {
            events = new ArrayList<>(copyOfExistentCompilation.getEvents());
            eventIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());
        }

        List<EventShortDto> eventsShortDtos = createEventShortDtos(events, eventIds);

        copyOfExistentCompilation = compilationRepository.save(copyOfExistentCompilation);

        return compilationMapper.convertCompilation(copyOfExistentCompilation, eventsShortDtos);
    }

    private Compilation createCompilation(NewCompilationDto newCompilationDto) {
        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.isPinned())
                .events(new HashSet<>())
                .build();
    }

    private List<Event> findEvents(List<Long> eventIds) {
        if (eventIds != null && !eventIds.isEmpty()) {
            return eventRepository.findAllByIdIn(eventIds);
        } else {
            return List.of();
        }
    }

    private List<EventShortDto> createEventShortDtos(List<Event> events, List<Long> eventIds) {
        Map<Long, Integer> hitsByEvent = eventUtilService.getHitsByEvent(eventIds);
        Map<Long, Long> confirmedRequestCountById = eventUtilService.getConfirmedRequestCountById(eventIds);
        return events.stream()
                .map(event -> eventMapper.convertEventToShortDto(event,
                        hitsByEvent.getOrDefault(event.getId(), 0),
                        confirmedRequestCountById.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }
}
