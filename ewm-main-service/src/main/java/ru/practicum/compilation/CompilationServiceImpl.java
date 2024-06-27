package ru.practicum.compilation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.RatingDto;
import ru.practicum.exception.NotFoundException;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final EventUtilService eventUtilService;
    private final EventUserRatingRepository eventUserRatingRepository;

    @Autowired
    public CompilationServiceImpl(CompilationRepository compilationRepository,
                                  EventRepository eventRepository,
                                  CompilationMapper compilationMapper,
                                  EventMapper eventMapper,
                                  EventUtilService eventUtilService,
                                  EventUserRatingRepository eventUserRatingRepository) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.compilationMapper = compilationMapper;
        this.eventMapper = eventMapper;
        this.eventUtilService = eventUtilService;
        this.eventUserRatingRepository = eventUserRatingRepository;
    }

    @Override
    @Transactional
    public CompilationDto add(NewCompilationDto newCompilationDto) {
        Compilation compilation = createCompilation(newCompilationDto);
        List<EventShortDto> eventsShortDtos = new ArrayList<>();
        List<Long> eventIds = newCompilationDto.getEvents();
        if (eventIds != null && !eventIds.isEmpty()) {
            List<Event> events = findEvents(eventIds);
            if (!events.isEmpty()) {
                compilation.getEvents().addAll(events);
            }
            eventsShortDtos = createEventShortDtos(events);
        }
        compilation = compilationRepository.save(compilation);

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

        List<Long> eventIds;
        List<Event> events = new ArrayList<>();

        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            copyOfExistentCompilation.setEvents(new HashSet<>());
            eventIds = updateCompilationRequest.getEvents();
            events = findEvents(eventIds);
            copyOfExistentCompilation.getEvents().addAll(events);
        } else if (copyOfExistentCompilation.getEvents() != null && !copyOfExistentCompilation.getEvents().isEmpty()) {
            events = new ArrayList<>(copyOfExistentCompilation.getEvents());
        }

        List<EventShortDto> eventsShortDtos = createEventShortDtos(events);

        copyOfExistentCompilation = compilationRepository.save(copyOfExistentCompilation);

        return compilationMapper.convertCompilation(copyOfExistentCompilation, eventsShortDtos);
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

                List<EventShortDto> eventShortDtos = new ArrayList<>();

                if (eventsOfCompilation != null && !eventsOfCompilation.isEmpty()) {

                    eventShortDtos = createEventShortDtos(new ArrayList<>(eventsOfCompilation));

                }

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

            eventShortDtos = createEventShortDtos(new ArrayList<>(events));
        }

        return compilationMapper.convertCompilation(compilation, eventShortDtos);
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

    private List<EventShortDto> createEventShortDtos(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Integer> hitsByEvent = eventUtilService.getHitsByEvent(eventIds);
        Map<Long, Long> confirmedRequestCountById = eventUtilService.getConfirmedRequestCountById(eventIds);
        Map<Long, Long> usersRating = eventUserRatingRepository.findUsersRatingByUserIds(events.stream()
                .map(el -> el.getInitiator().getId())
                .distinct()
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(
                        RatingDto::getId,  // eventId
                        RatingDto::getRating    // rating
                ));
        Map<Long, Long> eventsRating = eventUserRatingRepository.findRatingOfEventsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        RatingDto::getId,  // eventId
                        RatingDto::getRating    // rating
                ));

        return events.stream()
                .map(event -> eventMapper.convertEventToShortDto(event,
                        hitsByEvent.getOrDefault(event.getId(), 0),
                        confirmedRequestCountById.getOrDefault(event.getId(), 0L),
                        eventsRating.getOrDefault(event.getId(), 0L),
                        usersRating.getOrDefault(event.getInitiator().getId(), 0L)))
                .collect(Collectors.toList());
    }

}
