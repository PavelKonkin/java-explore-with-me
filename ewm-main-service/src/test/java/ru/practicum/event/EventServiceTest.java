package ru.practicum.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.client.StatClient;
import ru.practicum.event.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestMapper;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventUtilService eventUtilService;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ParticipationRequestRepository requestRepository;
    @Mock
    private ParticipationRequestMapper requestMapper;
    @Mock
    private StatClient statClient;
    @Mock
    private RatingService ratingService;
    @InjectMocks
    private EventServiceImpl eventService;

    private final Sort sort = Sort.by("createdOn").descending();
    Pageable page = new OffsetPage(0, 10, sort);

    private User user;
    private Event event1;
    private Event event1Updated;
    private Event event1ToSave;
    private Event event2;
    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventFullDto eventFullDto1;
    private EventFullDto eventFullDtoUpdated1;
    private EventFullDto eventFullDtoUpdatedPopulated1;
    private EventFullDto eventFullDtoPopulated;
    private Category category;
    private NewEventDto newEventDto;
    private Location locationToSave;
    private Location location;
    private ParticipationRequest participationRequest1;
    private ParticipationRequest participationRequest2;
    private ParticipationRequest participationRequestUpdated1;
    private ParticipationRequest participationRequestUpdated2;
    private ParticipationRequestDto participationRequestDto1;
    private ParticipationRequestDto participationRequestDto2;
    private ParticipationRequestDto participationRequestDtoUpdated1;
    private ParticipationRequestDto participationRequestDtoUpdated2;
    private UpdateEventUserRequest updateRequest;
    private EventRequestStatusUpdateRequest updateEventRequest;
    private EventRequestStatusUpdateResult updateEventResult;
    private final Map<Long, Integer> hitsByEvent = new HashMap<>();
    private final Map<Long, Long> confirmedRequestCount = new HashMap<>();
    private final long wrongId = 66L;

    private AdminEventParams adminEventParams;
    private Event event1Adm;
    private Event event1UpdatedAdm;
    private Event event2Adm;
    private EventFullDto eventFullDto1Adm;
    private EventFullDto eventFullDto2Adm;
    private EventFullDto eventFullDto1UpdatedAdm;
    private EventFullDto eventFullDto1ToUpdateAdm;
    private UpdateEventAdminRequest adminRequestToPublish;
    private UpdateEventAdminRequest adminRequestToCancel;


    private PublicEventParams publicEventParams;
    private Event event1Pub;
    private Event event2Pub;
    private Event event3NotPublished;
    private EventShortDto eventShortDto1Pub;
    private EventShortDto eventShortDto2Pub;
    private EventFullDto eventFullDtoPub;

    private final String uri = "/events";
    private final String ipAddress = "111.0.0.0";




    @BeforeEach
    public void setup() {
        category = Category.builder()
                .id(1L)
                .name("test")
                .build();
        CategoryDto categoryDto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
        locationToSave = Location.builder()
                .lon(22.22f)
                .lat(33.33f)
                .build();
        location = Location.builder()
                .id(1L)
                .lon(locationToSave.getLon())
                .lat(locationToSave.getLat())
                .build();
        LocationDto locationDto = LocationDto.builder()
                .lon(location.getLon())
                .lat(location.getLat())
                .build();
        newEventDto = NewEventDto.builder()
                .category(1L)
                .location(locationDto)
                .build();
        user = User.builder()
                .id(1L)
                .name("test")
                .email("test@user.email")
                .build();
        UserShortDto userDto = UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
        event1 = Event.builder()
                .id(1L)
                .initiator(user)
                .category(category)
                .location(location)
                .state(EventState.PENDING)
                .participantLimit(0)
//                .likes(Set.of())
//                .dislikes(Set.of())
                .build();
        event1ToSave = Event.builder()
                .initiator(user)
                .category(category)
                .location(location)
                .state(EventState.PENDING)
                .participantLimit(0)
                .build();
        event2 = Event.builder()
                .id(2L)
                .initiator(user)
                .participantLimit(2)
                .build();
        eventShortDto1 = EventShortDto.builder()
                .id(event1.getId())
                .build();
        eventShortDto2 = EventShortDto.builder()
                .id(event2.getId())
                .build();
        eventFullDto1 = EventFullDto.builder()
                .id(event1.getId())
                .category(categoryDto)
                .location(locationDto)
                .initiator(userDto)
                .state(EventState.PENDING)
                .build();
        hitsByEvent.put(1L, 1);
        hitsByEvent.put(2L, 2);
        confirmedRequestCount.put(1L, 1L);
        eventFullDtoPopulated = eventFullDto1.toBuilder()
                .views(1)
                .confirmedRequests(1L)
                .build();
        updateRequest = UpdateEventUserRequest.builder()
                .stateAction(StateUserAction.CANCEL_REVIEW)
                .build();
        eventFullDtoUpdated1 = eventFullDto1.toBuilder()
                .state(EventState.CANCELED)
                .build();
        eventFullDtoUpdatedPopulated1 = eventFullDtoUpdated1.toBuilder()
                .views(1)
                .confirmedRequests(1L)
                .build();
        event1Updated = event1.toBuilder()
                .state(EventState.PENDING)
                .build();
        participationRequest1 = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationRequestStatus.PENDING)
                .build();
        participationRequest2 = ParticipationRequest.builder()
                .id(2L)
                .status(ParticipationRequestStatus.PENDING)
                .build();
        participationRequestUpdated1 = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();
        participationRequestUpdated2 = ParticipationRequest.builder()
                .id(2L)
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();
        participationRequestDto1 = ParticipationRequestDto.builder()
                .id(participationRequest1.getId())
                .build();
        participationRequestDto2 = ParticipationRequestDto.builder()
                .id(participationRequest2.getId())
                .build();
        participationRequestDtoUpdated1 = ParticipationRequestDto.builder()
                .id(participationRequest1.getId())
                .status("CONFIRMED")
                .build();
        participationRequestDtoUpdated2 = ParticipationRequestDto.builder()
                .id(participationRequest2.getId())
                .status("CONFIRMED")
                .build();
        updateEventRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L, 2L))
                .status(RequestUpdateAction.CONFIRMED)
                .build();
        updateEventResult = EventRequestStatusUpdateResult.builder().build();


        Location locationSaved = Location.builder()
                .id(1L)
                .lat(22.22f)
                .lon(33.33f)
                .build();
        LocationDto locationDtoAdm = LocationDto.builder()
                .lat(locationSaved.getLat())
                .lon(locationSaved.getLon())
                .build();
        Category categoryAdm = Category.builder()
                .id(1L)
                .name("category")
                .build();
        CategoryDto categoryDtoAdm = CategoryDto.builder()
                .id(categoryAdm.getId())
                .name(categoryAdm.getName())
                .build();
        event1Adm = Event.builder()
                .id(1L)
                .title("test")
                .initiator(user)
                .state(EventState.PENDING)
                .build();
        event2Adm = Event.builder()
                .id(2L)
                .state(EventState.PUBLISHED)
                .build();
        eventFullDto1Adm = EventFullDto.builder()
                .id(event1Adm.getId())
                .build();
        eventFullDto2Adm = EventFullDto.builder()
                .id(event2Adm.getId())
                .build();
        eventFullDto1UpdatedAdm = EventFullDto.builder()
                .id(event1Adm.getId())
                .build();
        adminEventParams = AdminEventParams.builder()
                .from(0)
                .size(10)
                .build();
        adminRequestToPublish = UpdateEventAdminRequest.builder()
                .title("updated")
                .category(1L)
                .location(locationDto)
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();
        adminRequestToCancel = UpdateEventAdminRequest.builder()
                .title("updated")
                .stateAction(StateAdminAction.REJECT_EVENT)
                .build();
        eventFullDto1ToUpdateAdm = EventFullDto.builder()
                .id(event1Adm.getId())
                .title(adminRequestToPublish.getTitle())
                .state(EventState.CANCELED)
                .location(locationDto)
                .category(categoryDto)
                .build();
        eventFullDto1UpdatedAdm = EventFullDto.builder()
                .id(event1Adm.getId())
                .title(adminRequestToCancel.getTitle())
                .state(EventState.CANCELED)
                .views(1)
                .confirmedRequests(1)
                .location(locationDto)
                .category(categoryDto)
                .build();
        event1UpdatedAdm = Event.builder()
                .id(1L)
                .title(adminRequestToCancel.getTitle())
                .state(EventState.CANCELED)
                .initiator(user)
                .build();


        event1Pub = Event.builder()
                .id(1L)
                .state(EventState.PUBLISHED)
                .build();
        event2Pub = Event.builder()
                .id(2L)
                .build();
        event3NotPublished = Event.builder()
                .id(3L)
                .state(EventState.PENDING)
                .build();
        eventShortDto1Pub = EventShortDto.builder()
                .id(event1Pub.getId())
                .build();
        eventShortDto2Pub = EventShortDto.builder()
                .id(event2Pub.getId())
                .build();
        eventFullDtoPub = EventFullDto.builder()
                .id(event1Pub.getId())
                .build();
        publicEventParams = PublicEventParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("test")
                .onlyAvailable(false)
                .categories(List.of(1L))
                .paid(false)
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventShortDtos() {
        List<Long> eventIds = List.of(event1.getId(), event2.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findAllByInitiatorId(user.getId(), page)).thenReturn(List.of(event1, event2));
        when(ratingService.getUsersRating(List.of(1L))).thenReturn(new HashMap<>());
        when(ratingService.getEventsRating(List.of(1L, 2L))).thenReturn(new HashMap<>());
        when(eventMapper.convertEventToShortDto(event1,
                hitsByEvent.getOrDefault(event1.getId(), 0),
                confirmedRequestCount.getOrDefault(event1.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto1);
        when(eventMapper.convertEventToShortDto(event2,
                hitsByEvent.getOrDefault(event2.getId(), 0),
                confirmedRequestCount.getOrDefault(event2.getId(), 0L),
                0L, 0L))
                .thenReturn(eventShortDto2);
        when(eventUtilService.getHitsByEvent(eventIds)).thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds)).thenReturn(confirmedRequestCount);

        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(user.getId(), page);

        assertThat(List.of(eventShortDto1, eventShortDto2), is(actualListOfEventShortDtos));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findAllByInitiatorId(user.getId(), page);
        verify(eventMapper, times(2))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, times(1)).getHitsByEvent(eventIds);
        verify(eventUtilService, times(1)).getConfirmedRequestCountById(eventIds);
    }

    @Test
    public void getAll_whenNoEventsFound_thenReturnEmptyList() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findAllByInitiatorId(user.getId(), page)).thenReturn(List.of());

        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(user.getId(), page);

        assertThat(List.of(), is(actualListOfEventShortDtos));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findAllByInitiatorId(user.getId(), page);
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(any());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void getAll_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getAll(wrongId, page));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(eventRepository, never()).findAllByInitiatorId(wrongId, page);
        verify(eventMapper, never())
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(any());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void add_whenSuccessful_thenReturnEventFullDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(locationRepository.save(locationToSave)).thenReturn(location);
        when(eventRepository.save(event1ToSave)).thenReturn(event1);
        when(eventMapper.convertEventToFullDto(event1, 0, 0L,
                0L, 0L))
                .thenReturn(eventFullDto1);

        EventFullDto actualEventFullDto = eventService.add(user.getId(), newEventDto);

        assertThat(eventFullDto1, is(actualEventFullDto));
        verify(userRepository, times(1)).findById(user.getId());
        verify(categoryRepository, times(1)).findById(category.getId());
        verify(locationRepository, times(1)).save(locationToSave);
        verify(eventRepository, times(1)).save(event1ToSave);
        verify(eventMapper, times(1)).convertEventToFullDto(event1,0, 0L,
                0L, 0L);
    }

    @Test
    public void add_whenUserNotFound_thenThrowsException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.add(wrongId, newEventDto));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(categoryRepository, never()).findById(anyLong());
        verify(locationRepository, never()).save(any(Location.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void add_whenCategoryNotFound_thenThrownException() {
        newEventDto.setCategory(wrongId);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.add(user.getId(), newEventDto));
        assertThat(exception.getMessage(), is("Category with id="
                + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(categoryRepository, times(1)).findById(wrongId);
        verify(locationRepository, never()).save(any(Location.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1,
                hitsByEvent.getOrDefault(event1.getId(), 0),
                confirmedRequestCount.getOrDefault(event1.getId(), 0L),
                0L, 0L))
                .thenReturn(eventFullDtoPopulated);
        when(eventUtilService.getHitsByEvent(List.of(event1.getId()))).thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(List.of(event1.getId())))
                .thenReturn(confirmedRequestCount);

        EventFullDto actualEventFullDto = eventService.get(event1.getId(), user.getId());

        assertThat(eventFullDtoPopulated, is(actualEventFullDto));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(eventMapper, times(1)).convertEventToFullDto(event1,
                hitsByEvent.getOrDefault(event1.getId(), 0),
                confirmedRequestCount.getOrDefault(event1.getId(), 0L),
                0L, 0L);
        verify(eventUtilService, times(1)).getHitsByEvent(List.of(event1.getId()));
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(List.of(event1.getId()));
    }

    @Test
    public void get_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, event1.getId()));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(eventRepository, never()).findByIdAndInitiatorId(event1.getId(), wrongId);
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void get_whenEventNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(wrongId, user.getId()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(user.getId(), wrongId));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(wrongId, user.getId());
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void update_whenSuccessful_thenReturnUpdatedEventFullDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(eventRepository.save(event1Updated)).thenReturn(event1Updated);
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1Updated,0, 0L,
                0L, 0L))
                .thenReturn(eventFullDtoUpdated1);

        EventFullDto actualEventFullDto = eventService.update(user.getId(), event1.getId(), updateRequest);

        assertThat(eventFullDtoUpdated1, is(actualEventFullDto));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(eventRepository, times(1)).save(event1Updated);
        verify(eventMapper, times(1)).convertEventToFullDto(event1Updated,0, 0L,
                0L, 0L);
    }

    @Test
    public void update_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.update(wrongId, event1.getId(), updateRequest));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(eventRepository, never()).findByIdAndInitiatorId(event1.getId(), wrongId);
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void update_whenEventNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(wrongId, user.getId()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.update(user.getId(), wrongId, updateRequest));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(wrongId, user.getId());
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void update_whenEventPublished_thenThrownException() {
        event1.setState(EventState.PUBLISHED);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.update(user.getId(), event1.getId(), updateRequest));
        assertThat(exception.getMessage(), is("Only pending or canceled events can be changed"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void update_whenCancelAlreadyCanceledEvent_thenReturnSameEventFullDto() {
        event1.setState(EventState.CANCELED);
        updateRequest.setStateAction(StateUserAction.CANCEL_REVIEW);
        event1Updated.setState(EventState.CANCELED);
        eventFullDtoUpdated1.setState(EventState.CANCELED);
        eventFullDtoUpdatedPopulated1.setState(EventState.CANCELED);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(eventRepository.save(event1Updated)).thenReturn(event1Updated);
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1Updated,0, 0L,
                0L, 0L))
                .thenReturn(eventFullDtoUpdated1);

        EventFullDto actualEventFullDto = eventService.update(user.getId(), event1.getId(), updateRequest);

        assertThat(eventFullDtoUpdated1, is(actualEventFullDto));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(eventRepository, times(1)).save(event1Updated);
        verify(eventMapper, times(1)).convertEventToFullDto(event1Updated,0, 0L,
                0L, 0L);
    }

    @Test
    public void update_whenSentToReviewPendingEvent_thenReturnSameEventFullDto() {
        updateRequest.setStateAction(StateUserAction.SEND_TO_REVIEW);
        event1Updated.setState(EventState.PENDING);
        eventFullDtoUpdated1.setState(EventState.PENDING);
        eventFullDtoUpdatedPopulated1.setState(EventState.PENDING);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(eventRepository.save(event1Updated)).thenReturn(event1Updated);
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1Updated, 0,0L,
                0L, 0L))
                .thenReturn(eventFullDtoUpdated1);

        EventFullDto actualEventFullDto = eventService.update(user.getId(), event1.getId(), updateRequest);

        assertThat(eventFullDtoUpdated1, is(actualEventFullDto));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(eventRepository, times(1)).save(event1Updated);
        verify(eventMapper, times(1)).convertEventToFullDto(event1Updated, 0, 0L,
                0L, 0L);
    }

    @Test
    public void update_whenUpdateToNotFoundCategory_thenThrownException() {
        updateRequest.setCategory(wrongId);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(categoryRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.update(user.getId(), event1.getId(), updateRequest));
        assertThat(exception.getMessage(), is("Category with id="
                + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(categoryRepository, times(1)).findById(wrongId);
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(anyList());
        verify(eventUtilService, never()).getConfirmedRequestCountById(anyList());
    }

    @Test
    public void getRequests_whenSuccessful_thenReturnListOfParticipationRequestDtos() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(requestRepository.findAllByEventIdAndEventInitiatorId(event1.getId(), user.getId()))
                .thenReturn(List.of(participationRequest1, participationRequest2));
        when(requestMapper.convertParticipationRequest(participationRequest1)).thenReturn(participationRequestDto1);
        when(requestMapper.convertParticipationRequest(participationRequest2)).thenReturn(participationRequestDto2);

        List<ParticipationRequestDto> actualListOfParticipationRequestDtos = eventService
                .getRequests(user.getId(), event1.getId());

        assertThat(List.of(participationRequestDto1, participationRequestDto2),
                is(actualListOfParticipationRequestDtos));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(requestRepository, times(1))
                .findAllByEventIdAndEventInitiatorId(event1.getId(), user.getId());
        verify(requestMapper, times(2))
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void getRequests_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getRequests(wrongId, event1.getId()));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(eventRepository, never())
                .findByIdAndInitiatorId(event1.getId(), wrongId);
        verify(requestRepository, never())
                .findAllByEventIdAndEventInitiatorId(event1.getId(), wrongId);
        verify(requestMapper, never())
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void getRequests_whenEventNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(wrongId, user.getId()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getRequests(user.getId(), wrongId));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(wrongId, user.getId());
        verify(requestRepository, never())
                .findAllByEventIdAndEventInitiatorId(wrongId, user.getId());
        verify(requestMapper, never())
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequests_whenAllConfirmed_thenReturnEventRequestStatusUpdateResult() {
        updateEventResult
                .setConfirmedRequests(List.of(participationRequestDtoUpdated1, participationRequestDtoUpdated2));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(requestRepository.findAllByIdIn(updateEventRequest.getRequestIds()))
                .thenReturn(List.of(participationRequest1, participationRequest2));
        when(requestRepository.findAllByEventIdAndStatus(event1.getId(), ParticipationRequestStatus.CONFIRMED))
                .thenReturn(List.of());
        when(requestRepository.saveAll(List.of(participationRequestUpdated1, participationRequestUpdated2)))
                .thenReturn(List.of(participationRequestUpdated1, participationRequestUpdated2));
        when(requestMapper.convertParticipationRequest(participationRequestUpdated1))
                .thenReturn(participationRequestDtoUpdated1);
        when(requestMapper.convertParticipationRequest(participationRequestUpdated2))
                .thenReturn(participationRequestDtoUpdated2);

        EventRequestStatusUpdateResult actualResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateEventRequest);

        assertThat(updateEventResult, is(actualResult));
        assertThat(actualResult.getConfirmedRequests().size(), is(2));
        assertThat(actualResult.getConfirmedRequests().get(0).getStatus(),
                is(ParticipationRequestStatus.CONFIRMED.name()));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(requestRepository, times(1)).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, times(1))
                .findAllByEventIdAndStatus(event1.getId(), ParticipationRequestStatus.CONFIRMED);
        verify(requestRepository, times(1))
                .saveAll(List.of(participationRequestUpdated1, participationRequestUpdated2));
        verify(requestMapper, times(2))
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequests_whenAllRejected_thenReturnEventRequestStatusUpdateResult() {
        participationRequestDtoUpdated1.setStatus(ParticipationRequestStatus.REJECTED.name());
        participationRequestDtoUpdated2.setStatus(ParticipationRequestStatus.REJECTED.name());
        updateEventRequest.setStatus(RequestUpdateAction.REJECTED);
        updateEventResult
                .setRejectedRequests(List.of(participationRequestDtoUpdated1, participationRequestDtoUpdated2));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(requestRepository.findAllByIdIn(updateEventRequest.getRequestIds()))
                .thenReturn(List.of(participationRequest1, participationRequest2));
        when(requestRepository.saveAll(List.of(participationRequestUpdated1, participationRequestUpdated2)))
                .thenReturn(List.of(participationRequestUpdated1, participationRequestUpdated2));
        when(requestMapper.convertParticipationRequest(participationRequestUpdated1))
                .thenReturn(participationRequestDtoUpdated1);
        when(requestMapper.convertParticipationRequest(participationRequestUpdated2))
                .thenReturn(participationRequestDtoUpdated2);

        EventRequestStatusUpdateResult actualResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateEventRequest);

        assertThat(updateEventResult, is(actualResult));
        assertThat(actualResult.getRejectedRequests().size(), is(2));
        assertThat(actualResult.getRejectedRequests().get(0).getStatus(),
                is(ParticipationRequestStatus.REJECTED.name()));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(requestRepository, times(1)).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, times(1))
                .saveAll(List.of(participationRequestUpdated1, participationRequestUpdated2));
        verify(requestMapper, times(2))
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequests_whenOneRejectedAndOneConfirmed_thenReturnEventRequestStatusUpdateResult() {
        participationRequestDtoUpdated1.setStatus(ParticipationRequestStatus.CONFIRMED.name());
        participationRequestDtoUpdated2.setStatus(ParticipationRequestStatus.REJECTED.name());

        updateEventResult
                .setRejectedRequests(List.of(participationRequestDtoUpdated2));
        updateEventResult
                .setConfirmedRequests(List.of(participationRequestDtoUpdated1));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event2.getId(), user.getId()))
                .thenReturn(Optional.of(event2));
        when(requestRepository.findAllByIdIn(updateEventRequest.getRequestIds()))
                .thenReturn(List.of(participationRequest1, participationRequest2));
        when(requestRepository.findAllByEventIdAndStatus(event2.getId(), ParticipationRequestStatus.CONFIRMED))
                .thenReturn(List.of(participationRequest1));
        when(requestRepository.saveAll(List.of(participationRequestUpdated1)))
                .thenReturn(List.of(participationRequestUpdated1));
        when(requestRepository.saveAll(List.of(participationRequestUpdated2)))
                .thenReturn(List.of(participationRequestUpdated2));
        when(requestMapper.convertParticipationRequest(participationRequestUpdated1))
                .thenReturn(participationRequestDtoUpdated1);
        when(requestMapper.convertParticipationRequest(participationRequestUpdated2))
                .thenReturn(participationRequestDtoUpdated2);

        EventRequestStatusUpdateResult actualResult = eventService
                .updateRequests(user.getId(), event2.getId(), updateEventRequest);

        assertThat(updateEventResult, is(actualResult));
        assertThat(actualResult.getRejectedRequests().size(), is(1));
        assertThat(actualResult.getRejectedRequests().get(0).getStatus(),
                is(ParticipationRequestStatus.REJECTED.name()));
        assertThat(actualResult.getConfirmedRequests().size(), is(1));
        assertThat(actualResult.getConfirmedRequests().get(0).getStatus(),
                is(ParticipationRequestStatus.CONFIRMED.name()));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event2.getId(), user.getId());
        verify(requestRepository, times(1)).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, times(1))
                .saveAll(List.of(participationRequestUpdated1));
        verify(requestRepository, times(1))
                .saveAll(List.of(participationRequestUpdated2));
        verify(requestMapper, times(2))
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequest_whenRequestHasStatusOtherThanPending_thenThrownException() {
        participationRequest1.setStatus(ParticipationRequestStatus.CONFIRMED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(event1.getId(), user.getId()))
                .thenReturn(Optional.of(event1));
        when(requestRepository.findAllByIdIn(updateEventRequest.getRequestIds()))
                .thenReturn(List.of(participationRequest1, participationRequest2));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateRequests(user.getId(), event1.getId(), updateEventRequest));
        assertThat(exception.getMessage(), is("Not all requests are in PENDING status"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1))
                .findByIdAndInitiatorId(event1.getId(), user.getId());
        verify(requestRepository, times(1)).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, never())
                .findAllByEventIdAndStatus(event1.getId(), ParticipationRequestStatus.CONFIRMED);
        verify(requestRepository, never()).saveAll(anyList());
        verify(requestMapper, never())
                .convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequests_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateRequests(wrongId, event1.getId(), updateEventRequest));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
        verify(userRepository, times(1)).findById(wrongId);
        verify(eventRepository, never()).findByIdAndInitiatorId(event1.getId(), wrongId);
        verify(requestRepository, never()).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, never())
                .findAllByEventIdAndStatus(event1.getId(), ParticipationRequestStatus.CONFIRMED);
        verify(requestRepository, never()).saveAll(anyList());
        verify(requestMapper, never()).convertParticipationRequest(any(ParticipationRequest.class));
    }

    @Test
    public void updateRequests_whenEventNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(wrongId, user.getId()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateRequests(user.getId(), wrongId, updateEventRequest));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, never()).findByIdAndInitiatorId(event1.getId(), wrongId);
        verify(requestRepository, never()).findAllByIdIn(updateEventRequest.getRequestIds());
        verify(requestRepository, never())
                .findAllByEventIdAndStatus(event1.getId(), ParticipationRequestStatus.CONFIRMED);
        verify(requestRepository, never()).saveAll(anyList());
        verify(requestMapper, never()).convertParticipationRequest(any(ParticipationRequest.class));
    }


    @Test
    public void adm_getAll_whenSuccessful_thenReturnListOfEventFullDtos() {
        event1Adm.setInitiator(user);
        event2Adm.setInitiator(user);
        List<Long> eventIds = List.of(event1Adm.getId(), event2Adm.getId());
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event1Adm, event2Adm)));
        when(eventUtilService.getHitsByEvent(eventIds)).thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds)).thenReturn(confirmedRequestCount);
        when(ratingService.getUsersRating(List.of(1L))).thenReturn(new HashMap<>());
        when(ratingService.getEventsRating(List.of(1L, 2L))).thenReturn(new HashMap<>());
        when(eventMapper.convertEventToFullDto(event1Adm, hitsByEvent.getOrDefault(event1Adm.getId(), 0),
                confirmedRequestCount.getOrDefault(event1Adm.getId(), 0L),
                0L, 0L))
                .thenReturn(eventFullDto1Adm);
        when(eventMapper.convertEventToFullDto(event2Adm, hitsByEvent.getOrDefault(event2Adm.getId(), 0),
                confirmedRequestCount.getOrDefault(event2Adm.getId(), 0L),
                0L, 0L))
                .thenReturn(eventFullDto2Adm);

        List<EventFullDto> actualListOfEventFullDto = eventService.getAll(adminEventParams);

        assertThat(List.of(eventFullDto1Adm, eventFullDto2Adm), is(actualListOfEventFullDto));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, times(1)).getHitsByEvent(eventIds);
        verify(eventUtilService, times(1)).getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2))
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void adm_getAll_whenNoneEventFound_thenReturnEmptyList() {
        List<Long> eventIds = List.of();
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(ratingService.getUsersRating(List.of())).thenReturn(new HashMap<>());
        List<EventFullDto> actualListOfEventFullDto = eventService.getAll(adminEventParams);

        assertThat(List.of(), is(actualListOfEventFullDto));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, never()).getHitsByEvent(eventIds);
        verify(eventUtilService, never()).getConfirmedRequestCountById(eventIds);
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void adm_patch_whenSuccessfulRejectRequest_thenReturnUpdatedEventFullDto() {
        eventFullDto1UpdatedAdm.setViews(0);
        eventFullDto1UpdatedAdm.setConfirmedRequests(0L);

        when(eventRepository.findById(event1Adm.getId())).thenReturn(Optional.of(event1Adm));
        when(eventRepository.save(event1UpdatedAdm)).thenReturn(event1UpdatedAdm);
        when(ratingService.getEventRating(1L)).thenReturn(0L);
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1UpdatedAdm,0, 0L,
                0L, 0L))
                .thenReturn(eventFullDto1ToUpdateAdm);

        EventFullDto actualEventFullDto = eventService.patch(event1Adm.getId(), adminRequestToCancel);

        assertThat(eventFullDto1UpdatedAdm, is(actualEventFullDto));
        assertThat(adminRequestToCancel.getTitle(), is(actualEventFullDto.getTitle()));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
        verify(eventRepository, times(1)).findById(event1Adm.getId());
        verify(eventMapper, times(1)).convertEventToFullDto(event1UpdatedAdm,0, 0L,
                0L, 0L);
    }

    @Test
    public void adm_patch_whenPublishEventWithWrongState_thenThrownException() {
        when(eventRepository.findById(event2Adm.getId())).thenReturn(Optional.of(event2Adm));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event2Adm.getId(), adminRequestToPublish));
        assertThat(exception.getMessage(),
                is("Cannot publish the event because it's not in the right state: " + event2Adm.getState()));
        verify(eventRepository, times(1)).findById(event2Adm.getId());
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(List.of(event2Adm.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event2Adm.getId()));
    }

    @Test
    public void adm_patch_whenCancelEventWithWrongState_thenThrownException() {
        when(eventRepository.findById(event2Adm.getId())).thenReturn(Optional.of(event2Adm));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event2Adm.getId(), adminRequestToCancel));
        assertThat(exception.getMessage(),
                is("Cannot cancel the event because it's not in the right state: " + event2Adm.getState()));
        verify(eventRepository, times(1)).findById(event2Adm.getId());
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(List.of(event2Adm.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event2Adm.getId()));
    }

    @Test
    public void pub_getAll_whenSortByViews_thenReturnListOfEventShortDto() {
        List<Long> eventIds = List.of(event1Pub.getId(), event2Pub.getId());
        event1Pub.setInitiator(user);
        event2Pub.setInitiator(user);
        publicEventParams.setSort("VIEWS");

        doNothing().when(statClient)
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event1Pub, event2Pub));
        when(eventUtilService.getHitsByEvent(eventIds)).thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds)).thenReturn(confirmedRequestCount);
        when(ratingService.getUsersRating(List.of(1L))).thenReturn(new HashMap<>());
        when(ratingService.getEventsRating(List.of(1L, 2L))).thenReturn(new HashMap<>());
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

        List<EventShortDto> actualEventShortDtos = eventService.getAll(publicEventParams, uri, ipAddress);

        assertThat(List.of(eventShortDto1Pub, eventShortDto2Pub), is(actualEventShortDtos));
        verify(statClient, times(1))
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventRepository, times(1)).findAll(any(Specification.class));
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void pub_getAll_whenSortByDate_thenReturnListOfEventShortDto() {
        List<Long> eventIds = List.of(event1Pub.getId(), event2Pub.getId());
        event1Pub.setInitiator(user);
        event2Pub.setInitiator(user);

        doNothing().when(statClient)
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event1Pub, event2Pub)));
        when(eventUtilService.getHitsByEvent(eventIds))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds))
                .thenReturn(confirmedRequestCount);
        when(ratingService.getUsersRating(List.of(1L))).thenReturn(new HashMap<>());
        when(ratingService.getEventsRating(List.of(1L, 2L))).thenReturn(new HashMap<>());
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

        List<EventShortDto> actualEventShortDtos = eventService.getAll(publicEventParams, uri, ipAddress);

        assertThat(List.of(eventShortDto1Pub, eventShortDto2Pub), is(actualEventShortDtos));
        verify(statClient, times(1))
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2))
                .convertEventToShortDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
    }

    @Test
    public void pub_get_whenSuccessful_thenReturnEventFullDto() {
        event1Pub.setInitiator(user);

        when(eventRepository.findByIdAndState(event1Pub.getId(), EventState.PUBLISHED))
                .thenReturn(Optional.of(event1Pub));
        when(ratingService.getUserRating(1L)).thenReturn(0L);
        when(ratingService.getEventRating(1L)).thenReturn(0L);
        when(eventMapper.convertEventToFullDto(event1Pub,
                hitsByEvent.getOrDefault(event1Pub.getId(), 0),
                confirmedRequestCount.getOrDefault(event1Pub.getId(), 0L),
                0L,0L))
                .thenReturn(eventFullDtoPub);
        doNothing().when(statClient)
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        when(eventUtilService.getHitsByEvent(List.of(event1Pub.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService
                .getConfirmedRequestCountById(List.of(event1Pub.getId())))
                .thenReturn(confirmedRequestCount);

        EventFullDto actualEventFullDto = eventService.get(event1Pub.getId(), uri, ipAddress);

        assertThat(eventFullDtoPub, is(actualEventFullDto));
        verify(eventRepository, times(1))
                .findByIdAndState(event1Pub.getId(), EventState.PUBLISHED);
        verify(eventMapper, times(1)).convertEventToFullDto(event1,
                hitsByEvent.getOrDefault(event1Pub.getId(), 0),
                confirmedRequestCount.getOrDefault(event1Pub.getId(), 0L),
                0L, 0L);
        verify(statClient, times(1))
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, times(1))
                .getHitsByEvent(List.of(event1Pub.getId()));
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(List.of(event1Pub.getId()));
    }

    @Test
    public void pub_get_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;
        when(eventRepository.findByIdAndState(wrongId, EventState.PUBLISHED)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, uri, ipAddress));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
        verify(eventRepository, times(1)).findByIdAndState(wrongId, EventState.PUBLISHED);
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(statClient, never())
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(wrongId));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(wrongId));
    }


    @Test
    public void pub_get_whenEventNotPublished_thenThrownException() {
        when(eventRepository.findByIdAndState(event3NotPublished.getId(), EventState.PUBLISHED))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(event3NotPublished.getId(), uri, ipAddress));
        assertThat(exception.getMessage(), is("Event with id=" + event3NotPublished.getId() + " was not found"));
        verify(eventRepository, times(1))
                .findByIdAndState(event3NotPublished.getId(), EventState.PUBLISHED);
        verify(eventMapper, never())
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong(), anyLong(), anyLong());
        verify(statClient, never())
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(event3NotPublished.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event3NotPublished.getId()));
    }
}
