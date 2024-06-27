package ru.practicum.compilation;

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
import ru.practicum.category.Category;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.user.User;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompilationServiceTest {
    @Mock
    private CompilationRepository compilationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CompilationMapper compilationMapper;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventUtilService eventUtilService;
    @Mock
    EventUserRatingRepository eventUserRatingRepository;
    @InjectMocks
    private CompilationServiceImpl compilationService;

    private NewCompilationDto newCompilationDto;
    private NewCompilationDto newCompilationDtoWithoutEvents;
    private Compilation compilation;
    private Compilation compilationWithoutEvents;
    private Compilation compilationToSave;
    private Compilation compilationToSaveWithoutEvents;
    private CompilationDto compilationDto;
    private CompilationDto compilationDtoWithoutEvents;
    private CompilationDto updatedCompilationDto;
    private Event event1;
    private Event event2;
    private Event event3;
    private Category category;
    private CategoryDto categoryDto;
    private User initiator;
    private UserShortDto initiatorDto;
    private Location location;
    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventShortDto eventShortDto3;
    private UpdateCompilationRequest updateCompilationRequest;
    private Compilation updatedCompilation;
    private final Map<Long, Integer> hitsByEvent = new HashMap<>();
    private final Map<Long, Long> confirmedRequestCount = new HashMap<>();


    private Compilation compilation1Pub;
    private Compilation compilation2Pub;
    private CompilationDto compilationDto1Pub;
    private CompilationDto compilationDto2Pub;
    private Event event1Pub;
    private Event event2Pub;
    private ResponseEntity<Object> response;
    private ResponseEntity<Object> response2;
    private ParticipationRequest requestPub;
    private EventShortDto eventShortDto1Pub;
    private EventShortDto eventShortDto2Pub;
    private final Sort sort = Sort.by("id").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);



    @BeforeEach
    public void setup() {
        newCompilationDto = NewCompilationDto.builder()
                .events(List.of(1L, 2L))
                .title("test compilation")
                .pinned(false)
                .build();
        newCompilationDtoWithoutEvents = NewCompilationDto.builder()
                .title("test compilation no events")
                .pinned(false)
                .build();
        compilation = Compilation.builder()
                .id(1L)
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.isPinned())
                .build();
        compilationWithoutEvents = Compilation.builder()
                .id(1L)
                .title(newCompilationDtoWithoutEvents.getTitle())
                .pinned(newCompilationDtoWithoutEvents.isPinned())
                .build();
        compilationToSave = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.isPinned())
                .build();
        compilationToSaveWithoutEvents = Compilation.builder()
                .title(newCompilationDtoWithoutEvents.getTitle())
                .pinned(newCompilationDtoWithoutEvents.isPinned())
                .build();
        compilationDto = CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .build();
        compilationDtoWithoutEvents = CompilationDto.builder()
                .id(compilationWithoutEvents.getId())
                .title(compilationWithoutEvents.getTitle())
                .pinned(compilationWithoutEvents.isPinned())
                .build();
        category = Category.builder()
                .id(1L)
                .name("test caegory")
                .build();
        categoryDto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
        initiator = User.builder()
                .id(1L)
                .email("test@email.test")
                .name("test user")
                .build();
        initiatorDto = UserShortDto.builder()
                .id(initiator.getId())
                .name(initiator.getName())
                .build();
        location = Location.builder()
                .id(1L)
                .lon(22.22f)
                .lat(33.33f)
                .build();
        event1 = Event.builder()
                .id(1L)
                .title("event1")
                .annotation("annotation1")
                .description("description1")
                .participantLimit(0)
                .requestModeration(false)
                .eventDate(LocalDateTime.now().plusHours(5))
                .paid(false)
                .state(EventState.PENDING)
                .category(category)
                .initiator(initiator)
                .location(location)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .build();
        event2 = Event.builder()
                .id(2L)
                .title("event2")
                .annotation("annotation2")
                .description("description2")
                .participantLimit(0)
                .requestModeration(false)
                .eventDate(LocalDateTime.now().plusHours(5))
                .paid(false)
                .state(EventState.PENDING)
                .category(category)
                .initiator(initiator)
                .location(location)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .build();
        event3 = Event.builder()
                .id(3L)
                .title("event3")
                .annotation("annotation3")
                .description("description3")
                .participantLimit(0)
                .requestModeration(false)
                .eventDate(LocalDateTime.now().plusHours(5))
                .paid(false)
                .state(EventState.PENDING)
                .category(category)
                .initiator(initiator)
                .location(location)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .build();
        eventShortDto1 = EventShortDto.builder()
                .id(event1.getId())
                .initiator(initiatorDto)
                .category(categoryDto)
                .eventDate(event1.getEventDate())
                .paid(event1.isPaid())
                .annotation(event1.getAnnotation())
                .title(event1.getTitle())
                .views(1)
                .confirmedRequests(1)
                .build();
        eventShortDto2 = EventShortDto.builder()
                .id(event2.getId())
                .initiator(initiatorDto)
                .category(categoryDto)
                .eventDate(event2.getEventDate())
                .paid(event2.isPaid())
                .annotation(event2.getAnnotation())
                .title(event2.getTitle())
                .views(2)
                .confirmedRequests(0)
                .build();
        eventShortDto3 = EventShortDto.builder()
                .id(event3.getId())
                .initiator(initiatorDto)
                .category(categoryDto)
                .eventDate(event3.getEventDate())
                .paid(event3.isPaid())
                .annotation(event3.getAnnotation())
                .title(event3.getTitle())
                .views(4)
                .confirmedRequests(1)
                .build();
        updateCompilationRequest = UpdateCompilationRequest.builder()
                .title("updated title")
                .pinned(true)
                .events(List.of(3L))
                .build();
        updatedCompilation = compilation.toBuilder()
                .pinned(updateCompilationRequest.getPinned())
                .title(updateCompilationRequest.getTitle())
                .build();
        updatedCompilationDto = CompilationDto.builder()
                .id(updatedCompilation.getId())
                .title(updatedCompilation.getTitle())
                .pinned(updatedCompilation.isPinned())
                .build();
        hitsByEvent.put(1L, 1);
        hitsByEvent.put(2L, 2);
        confirmedRequestCount.put(1L, 1L);


        event1Pub = Event.builder()
                .id(1L)
                .build();
        event2Pub = Event.builder()
                .id(2L)
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
        eventShortDto1Pub = EventShortDto.builder()
                .id(event1Pub.getId())
                .views(1)
                .confirmedRequests(1)
                .build();
        eventShortDto2Pub = EventShortDto.builder()
                .id(event2Pub.getId())
                .views(2)
                .confirmedRequests(0)
                .build();
        requestPub = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationRequestStatus.CONFIRMED)
                .event(event1Pub)
                .created(LocalDateTime.now())
                .build();
        compilation1Pub = Compilation.builder()
                .id(1L)
                .title("comp1")
                .pinned(false)
                .events(Set.of(event1Pub))
                .build();
        compilation2Pub = Compilation.builder()
                .id(2L)
                .title("comp2")
                .pinned(false)
                .events(Set.of(event2Pub))
                .build();
        compilationDto1Pub = CompilationDto.builder()
                .id(compilation1Pub.getId())
                .title(compilation1Pub.getTitle())
                .pinned(compilation1Pub.isPinned())
                .events(List.of(eventShortDto1Pub))
                .build();
        compilationDto2Pub = CompilationDto.builder()
                .id(compilation2Pub.getId())
                .title(compilation2Pub.getTitle())
                .pinned(compilation2Pub.isPinned())
                .events(List.of(eventShortDto2Pub))
                .build();
    }

    @Test
    public void add_whenSuccessful_thenReturnCompilationDto() {
        List<Long> eventIds = List.of(1L, 2L);
        event1.setInitiator(initiator);
        event2.setInitiator(initiator);
        when(compilationRepository.save(compilationToSave)).thenReturn(compilation);
        when(eventRepository.findAllByIdIn(eventIds)).thenReturn(List.of(event1, event2));
        when(eventUtilService.getHitsByEvent(eventIds))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds))
                .thenReturn(confirmedRequestCount);
        when(eventUserRatingRepository.findRatingOfEventsByEventIds(List.of(1L, 2L))).thenReturn(List.of());
        when(eventUserRatingRepository.findUsersRatingByUserIds(List.of(1L))).thenReturn(List.of());
        when(eventMapper.convertEventToShortDto(event1, hitsByEvent.getOrDefault(event1.getId(), 0),
                confirmedRequestCount.getOrDefault(event1.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto1);
        when(eventMapper.convertEventToShortDto(event2, hitsByEvent.getOrDefault(event2.getId(), 0),
                confirmedRequestCount.getOrDefault(event2.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto2);
        when(compilationMapper.convertCompilation(compilation,
                List.of(eventShortDto1, eventShortDto2))).thenReturn(compilationDto);

        CompilationDto actualCompilationDto = compilationService.add(newCompilationDto);

        assertThat(compilationDto, is(actualCompilationDto));
        verify(compilationRepository, times(1)).save(compilationToSave);
        verify(eventRepository, times(1)).findAllByIdIn(eventIds);
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(compilationMapper, times(1))
                .convertCompilation(any(Compilation.class), anyList());
    }

    @Test
    public void add_whenCompilationWithoutEvents_thenReturnCompilationDtoWithoutEvents() {
        when(compilationRepository.save(compilationToSaveWithoutEvents)).thenReturn(compilationWithoutEvents);
        when(compilationMapper.convertCompilation(compilationWithoutEvents, List.of()))
                .thenReturn(compilationDtoWithoutEvents);

        CompilationDto actualCompilationDto = compilationService.add(newCompilationDtoWithoutEvents);

        assertThat(compilationDtoWithoutEvents, is(actualCompilationDto));
        verify(compilationRepository, times(1)).save(compilationToSaveWithoutEvents);
        verify(eventRepository, never()).findAllByIdIn(any());
        verify(eventUtilService, never())
                .getHitsByEvent(anyList());
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(anyList());
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(compilationMapper, times(1))
                .convertCompilation(any(Compilation.class), anyList());
    }

    @Test
    public void delete_whenSuccessful_thenDoNothing() {
        when(compilationRepository.findById(compilation.getId())).thenReturn(Optional.of(compilation));
        doNothing().when(compilationRepository).delete(compilation);

        compilationService.delete(compilation.getId());

        verify(compilationRepository, times(1)).findById(compilation.getId());
        verify(compilationRepository, times(1)).delete(compilation);
    }

    @Test
    public void delete_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66;
        when(compilationRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.delete(wrongId));

        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
        verify(compilationRepository, times(1)).findById(wrongId);
        verify(compilationRepository, never()).delete(any(Compilation.class));
    }

    @Test
    public void patch_whenSuccessful_thenReturnUpdatedCompilationDto() {
        List<Long> eventIds = updateCompilationRequest.getEvents();
        when(compilationRepository.findById(compilation.getId())).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(updatedCompilation)).thenReturn(updatedCompilation);
        when(eventRepository.findAllByIdIn(eventIds)).thenReturn(List.of(event3));
        when(eventUtilService.getHitsByEvent(eventIds))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds))
                .thenReturn(confirmedRequestCount);
        when(eventUserRatingRepository.findUsersRatingByUserIds(List.of(1L))).thenReturn(List.of());
        when(eventMapper.convertEventToShortDto(event3,
                hitsByEvent.getOrDefault(event3.getId(), 0),
                confirmedRequestCount.getOrDefault(event3.getId(), 0L),
                0L,
                0L))
                .thenReturn(eventShortDto3);
        when(compilationMapper.convertCompilation(updatedCompilation, List.of(eventShortDto3)))
                .thenReturn(updatedCompilationDto);

        CompilationDto actualUpdatedCompilationDto = compilationService
                .patch(compilation.getId(), updateCompilationRequest);

        assertThat(updatedCompilationDto, is(actualUpdatedCompilationDto));
        verify(compilationRepository, times(1)).findById(compilation.getId());
        verify(compilationRepository, times(1)).save(updatedCompilation);
        verify(eventRepository, times(1)).findAllByIdIn(eventIds);
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(1))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(compilationMapper, times(1))
                .convertCompilation(any(Compilation.class), anyList());
    }

    @Test
    public void patch_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66;
        when(compilationRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.patch(wrongId, updateCompilationRequest));

        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
        verify(compilationRepository, times(1)).findById(wrongId);
        verify(compilationRepository, never()).save(any(Compilation.class));
        verify(eventRepository, never()).findAllByIdIn(any());
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(wrongId));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(wrongId));
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(compilationMapper, never()).convertCompilation(any(Compilation.class), anyList());
    }


    @Test
    public void pub_getAll_whenSuccessful_thenReturnCompilationDto() {
        event1Pub.setInitiator(initiator);
        event2Pub.setInitiator(initiator);
        when(compilationRepository.findAllByPinned(false, page))
                .thenReturn(List.of(compilation1Pub, compilation2Pub));
        when(compilationMapper.convertCompilation(compilation1Pub, List.of(eventShortDto1Pub)))
                .thenReturn(compilationDto1Pub);
        when(compilationMapper.convertCompilation(compilation2Pub, List.of(eventShortDto2Pub)))
                .thenReturn(compilationDto2Pub);
        when(eventUtilService.getHitsByEvent(List.of(event1Pub.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getHitsByEvent(List.of(event2Pub.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(List.of(event1Pub.getId())))
                .thenReturn(confirmedRequestCount);
        when(eventUtilService.getConfirmedRequestCountById(List.of(event2Pub.getId())))
                .thenReturn(confirmedRequestCount);
        when(eventUserRatingRepository.findUsersRatingByUserIds(List.of(1L))).thenReturn(List.of());
        when(eventUserRatingRepository.findRatingOfEventsByEventIds(List.of(1L))).thenReturn(List.of());
        when(eventMapper.convertEventToShortDto(event1Pub,
                hitsByEvent.getOrDefault(event1Pub.getId(), 0),
                confirmedRequestCount.getOrDefault(event1Pub.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto1Pub);
        when(eventMapper.convertEventToShortDto(event2Pub,
                hitsByEvent.getOrDefault(event2Pub.getId(), 0),
                confirmedRequestCount.getOrDefault(event2Pub.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto2Pub);

        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(false, page);

        assertThat(List.of(compilationDto1Pub, compilationDto2Pub), is(actualListOfCompilationDto));
        verify(compilationRepository, times(1)).findAllByPinned(false, page);
        verify(compilationMapper, times(2))
                .convertCompilation(any(Compilation.class), anyList());
        verify(eventUtilService, times(2))
                .getHitsByEvent(anyList());
        verify(eventUtilService, times(2))
                .getConfirmedRequestCountById(anyList());
        verify(eventMapper, times(2))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void pub_getAll_whenNoCompilationFound_thenReturnEmptyList() {
        List<Long> eventIds = List.of(1L, 2L);
        when(compilationRepository.findAllByPinned(true, page))
                .thenReturn(List.of());

        List<CompilationDto> actualListOfCompilationDto = compilationService.getAll(true, page);

        assertThat(List.of(), is(actualListOfCompilationDto));
        verify(compilationRepository, times(1)).findAllByPinned(true, page);
        verify(compilationMapper, never()).convertCompilation(any(Compilation.class), anyList());
        verify(eventUtilService, never())
                .getHitsByEvent(anyList());
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void pub_get_whenSuccessful_thenReturnCompilationDto() {
        event1Pub.setInitiator(initiator);
        when(compilationRepository.findById(compilation1Pub.getId()))
                .thenReturn(Optional.of(compilation1Pub));
        when(compilationMapper.convertCompilation(compilation1Pub, List.of(eventShortDto1Pub)))
                .thenReturn(compilationDto1Pub);
        when(eventUtilService.getHitsByEvent(List.of(event1Pub.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(List.of(event1Pub.getId())))
                .thenReturn(confirmedRequestCount);
        when(eventUserRatingRepository.findUsersRatingByUserIds(List.of(1L))).thenReturn(List.of());
        when(eventUserRatingRepository.findRatingOfEventsByEventIds(List.of(1L))).thenReturn(List.of());
        when(eventMapper.convertEventToShortDto(event1Pub,
                hitsByEvent.getOrDefault(event1Pub.getId(), 0),
                confirmedRequestCount.getOrDefault(event1Pub.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto1Pub);

        CompilationDto actuaCompilationDto = compilationService.get(compilation1Pub.getId());

        assertThat(compilationDto1Pub, is(actuaCompilationDto));
        verify(compilationRepository, times(1)).findById(compilation1Pub.getId());
        verify(compilationMapper, times(1))
                .convertCompilation(any(Compilation.class), anyList());
        verify(eventUtilService, times(1))
                .getHitsByEvent(List.of(event1Pub.getId()));
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(List.of(event1Pub.getId()));
        verify(eventMapper, times(1))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void pub_get_whenCompilationNotFound_thenThrownException() {
        long wrongId = 66L;
        when(compilationRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> compilationService.get(wrongId));
        assertThat(exception.getMessage(), is("Compilation with id=" + wrongId + " was not found"));
        verify(compilationRepository, times(1)).findById(wrongId);
        verify(compilationMapper, never()).convertCompilation(any(Compilation.class), anyList());
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(wrongId));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(wrongId));
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }
}
