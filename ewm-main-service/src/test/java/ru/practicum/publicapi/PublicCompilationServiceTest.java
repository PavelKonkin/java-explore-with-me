package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.practicum.client.StatClient;
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
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicCompilationServiceTest {
    @Mock
    private CompilationRepository compilationRepository;
    @Mock
    private EventCompilationRepository eventCompilationRepository;
    @Mock
    private StatClient statClient;
    @Mock
    private CompilationMapper compilationMapper;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private EventUtilService eventUtilService;
    @InjectMocks
    private PublicCompilationServiceImpl compilationService;

    private Compilation compilation1;
    private Compilation compilation2;
    private CompilationDto compilationDto1;
    private CompilationDto compilationDto2;
    private Event event1;
    private Event event2;
    private ResponseEntity<Object> response;
    private ResponseEntity<Object> response2;
    private ParticipationRequest request;
    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventCompilation eventCompilation1;
    private EventCompilation eventCompilation2;
    private final Map<Long, Integer> hitsByEvent = new HashMap<>();
    private final Map<Long, Long> confirmedRequestCount = new HashMap<>();


    private static final String EVENTS_START = "1970-01-01 00:00:00";
    private static final String EVENTS_END = "2999-12-31 00:00:00";
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);

    @BeforeEach
    public void setup() {
        compilation1 = Compilation.builder()
                .id(1L)
                .title("comp1")
                .pinned(false)
                .build();
        compilation2 = Compilation.builder()
                .id(2L)
                .title("comp2")
                .pinned(false)
                .build();
        compilationDto1 = CompilationDto.builder()
                .id(compilation1.getId())
                .title(compilation1.getTitle())
                .pinned(compilation1.isPinned())
                .build();
        compilationDto2 = CompilationDto.builder()
                .id(compilation2.getId())
                .title(compilation2.getTitle())
                .pinned(compilation2.isPinned())
                .build();
        event1 = Event.builder()
                .id(1L)
                .build();
        event2 = Event.builder()
                .id(2L)
                .build();
        eventCompilation1 = EventCompilation.builder()
                .compilation(compilation1)
                .event(event1)
                .build();
        eventCompilation2 = EventCompilation.builder()
                .compilation(compilation2)
                .event(event2)
                .build();
        Map<String, Object> hit1 = new HashMap<>();
        hit1.put("hits", 1);
        hit1.put("uri", "/event/1");
        hit1.put("app", "app1");
        Map<String, Object> hit2 = new HashMap<>();
        hit2.put("hits", 2);
        hit2.put("uri", "/event/2");
        hit2.put("app", "app1");
        response = new ResponseEntity<>(List.of(hit1, hit2), HttpStatus.OK);
        response2 = new ResponseEntity<>(List.of(hit1), HttpStatus.OK);
        eventShortDto1 = EventShortDto.builder()
                .id(event1.getId())
                .views(1)
                .confirmedRequests(1)
                .build();
        eventShortDto2 = EventShortDto.builder()
                .id(event2.getId())
                .views(2)
                .confirmedRequests(0)
                .build();
        request = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationRequestStatus.CONFIRMED)
                .event(event1)
                .created(LocalDateTime.now())
                .build();
        hitsByEvent.put(1L, 1);
        hitsByEvent.put(2L, 2);
        confirmedRequestCount.put(1L, 1L);
    }

    @Test
    public void getAll_whenSuccessful_thenReturnCompilationDto() {
        List<Long> eventIds = List.of(1L, 2L);
        when(compilationRepository.findAllByPinned(false, page))
                .thenReturn(List.of(compilation1, compilation2));
        when(compilationMapper.convertCompilation(compilation1)).thenReturn(compilationDto1);
        when(compilationMapper.convertCompilation(compilation2)).thenReturn(compilationDto2);
        when(eventCompilationRepository.findAllByCompilationIdIn(eventIds))
                .thenReturn(List.of(eventCompilation1, eventCompilation2));
        when(eventUtilService.getHitsByEvent(eventIds))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds))
                .thenReturn(confirmedRequestCount);
        when(eventMapper.convertEventToShortDto(event1)).thenReturn(eventShortDto1);
        when(eventMapper.convertEventToShortDto(event2)).thenReturn(eventShortDto2);

        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(false, page);

        assertThat(List.of(compilationDto1, compilationDto2), is(actualListOfCompilationDto));
        verify(compilationRepository, times(1)).findAllByPinned(false, page);
        verify(compilationMapper, times(2)).convertCompilation(any(Compilation.class));
        verify(eventCompilationRepository, times(1)).findAllByCompilationIdIn(eventIds);
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2)).convertEventToShortDto(any(Event.class));
    }

    @Test
    public void getAll_whenNoCompilationFound_thenReturnEmptyList() {
        List<Long> eventIds = List.of(1L, 2L);
        when(compilationRepository.findAllByPinned(true, page))
                .thenReturn(List.of());

        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(true, page);

        assertThat(List.of(), is(actualListOfCompilationDto));
        verify(compilationRepository, times(1)).findAllByPinned(true, page);
        verify(compilationMapper, never()).convertCompilation(any(Compilation.class));
        verify(eventCompilationRepository, never()).findAllByCompilationIdIn(eventIds);
        verify(eventUtilService, never())
                .getHitsByEvent(anyList());
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, never()).convertEventToShortDto(any(Event.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnCompilationDto() {
        when(compilationRepository.findById(compilation1.getId()))
                .thenReturn(Optional.of(compilation1));
        when(compilationMapper.convertCompilation(compilation1)).thenReturn(compilationDto1);
        when(eventCompilationRepository.findAllByCompilationId(compilation1.getId()))
                .thenReturn(List.of(eventCompilation1));
        when(eventUtilService.getHitsByEvent(List.of(event1.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(List.of(event1.getId())))
                .thenReturn(confirmedRequestCount);
        when(eventMapper.convertEventToShortDto(event1)).thenReturn(eventShortDto1);

        CompilationDto actuaCompilationDto = compilationService.get(compilation1.getId());

        assertThat(compilationDto1, is(actuaCompilationDto));
        verify(compilationRepository, times(1)).findById(compilation1.getId());
        verify(compilationMapper, times(1)).convertCompilation(any(Compilation.class));
        verify(eventCompilationRepository, times(1))
                .findAllByCompilationId(compilation1.getId());
        verify(eventUtilService, times(1))
                .getHitsByEvent(List.of(event1.getId()));
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(List.of(event1.getId()));
        verify(eventMapper, times(1)).convertEventToShortDto(any(Event.class));
    }

    @Test
    public void get_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66L;
        when(compilationRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> compilationService.get(wrongId));
        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
        verify(compilationRepository, times(1)).findById(wrongId);
        verify(compilationMapper, never()).convertCompilation(any(Compilation.class));
        verify(eventCompilationRepository, never())
                .findAllByCompilationId(anyLong());
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(wrongId));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(wrongId));
        verify(eventMapper, never()).convertEventToShortDto(any(Event.class));
    }

}
