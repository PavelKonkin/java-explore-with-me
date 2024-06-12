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
import ru.practicum.eventcompilation.EventCompilation;
import ru.practicum.eventcompilation.EventCompilationRepository;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminCompilationServiceImpl implements AdminCompilationService {
    private final CompilationRepository compilationRepository;
    private final EventCompilationRepository eventCompilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;

    @Autowired
    public AdminCompilationServiceImpl(CompilationRepository compilationRepository,
                                       EventCompilationRepository eventCompilationRepository,
                                       EventRepository eventRepository,
                                       CompilationMapper compilationMapper,
                                       EventMapper eventMapper,
                                       EventUtilService eventUtilService) {
        this.compilationRepository = compilationRepository;
        this.eventCompilationRepository = eventCompilationRepository;
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
        CompilationDto eventShortDtos = getCompilationDto(compilation, eventIds);
        if (eventShortDtos != null) return eventShortDtos;

        return compilationMapper.convertCompilation(compilation);
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
        copyOfExistentCompilation = compilationRepository.save(copyOfExistentCompilation);

        List<Long> eventIds = updateCompilationRequest.getEvents();

        if (eventIds != null) {
            deleteEventCompilations(compId);

            CompilationDto eventShortDtos = getCompilationDto(copyOfExistentCompilation, eventIds);
            if (eventShortDtos != null) return eventShortDtos;
        }

        return compilationMapper.convertCompilation(copyOfExistentCompilation);
    }

    private CompilationDto getCompilationDto(Compilation copyOfExistentCompilation, List<Long> eventIds) {
        List<Event> events = findEvents(eventIds);

        if (!events.isEmpty()) {
            Map<Long, Integer> hitsByEvent = eventUtilService.getHitsByEvent(eventIds);
            Map<Long, Long> confirmedRequestCountById = eventUtilService.getConfirmedRequestCountById(eventIds);
            List<EventShortDto> eventShortDtos = createEventShortDtos(events, hitsByEvent, confirmedRequestCountById);

            saveEventCompilations(copyOfExistentCompilation, events);
            return updateCompilationWithEvents(copyOfExistentCompilation, eventShortDtos);
        }
        return null;
    }

    private void deleteEventCompilations(long compId) {
        eventCompilationRepository.deleteAllByCompilationId(compId);
    }

    private Compilation createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.isPinned())
                .build();
        return compilationRepository.save(compilation);
    }

    private List<Event> findEvents(List<Long> eventIds) {
        if (eventIds != null && !eventIds.isEmpty()) {
            return eventRepository.findAllByIdIn(eventIds);
        } else {
            return List.of();
        }
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

    private void saveEventCompilations(Compilation compilation, List<Event> events) {
        List<EventCompilation> eventCompilations = new ArrayList<>();
        for (Event event : events) {
            EventCompilation eventCompilation = EventCompilation.builder()
                    .compilation(compilation)
                    .event(event)
                    .build();
            eventCompilations.add(eventCompilation);
        }
        eventCompilationRepository.saveAll(eventCompilations);
    }

    private CompilationDto updateCompilationWithEvents(Compilation compilation, List<EventShortDto> eventShortDtos) {
        CompilationDto compilationDto = compilationMapper.convertCompilation(compilation);
        compilationDto.setEvents(eventShortDtos);
        return compilationDto;
    }
}
