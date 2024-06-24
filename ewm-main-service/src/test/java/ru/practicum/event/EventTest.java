package ru.practicum.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;
import ru.practicum.user.UserService;
import ru.practicum.user.dto.UserDto;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class EventTest {
    @Autowired
    private EventService eventService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ParticipationRequestRepository requestRepository;
    @Autowired
    private UserService userService;

    private final Sort sort = Sort.by("createdOn").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);
    private final Sort sortUsers = Sort.by("id").descending();
    private final Pageable pageUsers = new OffsetPage(0, 10, sortUsers);
    private final long wrongId = Long.MAX_VALUE;
    private User user;
    private User user2;
    private User user33;
    private User user4;
    private Category category;
    private NewEventDto newEventDto;
    private Event event1;
    private Event event2;
    private Event event3;
    private UpdateEventUserRequest updateEventUserRequest;
    private ParticipationRequest participationRequest1;
    private ParticipationRequest participationRequest2;
    private EventRequestStatusUpdateRequest updateRequest;


    private Event event1Adm;
    private Event event2Adm;
    private Event event3NotPublished;
    private AdminEventParams adminEventParams;
    private UpdateEventAdminRequest adminRequestPublish;
    private UpdateEventAdminRequest adminRequestCancel;


    private Event event1Pub;
    private Event event2Pub;
    private Event event3NotPublishedPub;
    private PublicEventParams publicEventParams;
    private final String uri = "/events";
    private final String ipAddress = "111.0.0.0";

    @BeforeEach
    public void setup() {
        category = Category.builder()
                .name("test")
                .build();
        category = categoryRepository.save(category);
        user = User.builder()
                .email("email@user.test")
                .name("test")
                .build();
        user = userRepository.save(user);
        user2 = User.builder()
                .email("email@user2.test")
                .name("test2")
                .build();
        user2 = userRepository.save(user2);
        user33 = User.builder()
                .email("email@user33.test")
                .name("test33")
                .build();
        user33 = userRepository.save(user33);
        user4 = User.builder()
                .email("email@user4.test")
                .name("test4")
                .build();
        user4 = userRepository.save(user4);
        User user3 = User.builder()
                .email("email@user3.test")
                .name("test3")
                .build();
        user3 = userRepository.save(user3);
        Location location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        event1 = Event.builder()
                .category(category)
                .state(EventState.PENDING)
                .initiator(user)
                .location(location)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test")
                .description("description")
                .annotation("annotation")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event1 = eventRepository.save(event1);
        event2 = Event.builder()
                .category(category)
                .state(EventState.PUBLISHED)
                .initiator(user)
                .location(location)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test2")
                .description("description2")
                .annotation("annotation2")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event2 = eventRepository.save(event2);
        event3 = event2.toBuilder()
                .id(null)
                .state(EventState.CANCELED)
                .build();
        event3 = eventRepository.save(event3);
        LocationDto locationDto = LocationDto.builder()
                .lon(22.22f)
                .lat(33.33f)
                .build();
        newEventDto = NewEventDto.builder()
                .eventDate(LocalDateTime.now().plusHours(3))
                .title("test")
                .annotation("annotation")
                .description("description")
                .location(locationDto)
                .category(category.getId())
                .build();
        updateEventUserRequest = UpdateEventUserRequest.builder()
                .stateAction(StateUserAction.CANCEL_REVIEW)
                .build();
        participationRequest1 = ParticipationRequest.builder()
                .status(ParticipationRequestStatus.PENDING)
                .event(event1)
                .requester(user2)
                .build();
        participationRequest1 = requestRepository.save(participationRequest1);
        participationRequest2 = ParticipationRequest.builder()
                .status(ParticipationRequestStatus.PENDING)
                .event(event1)
                .requester(user3)
                .build();
        participationRequest2 = requestRepository.save(participationRequest2);
        updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(participationRequest1.getId(), participationRequest2.getId()))
                .status(RequestUpdateAction.CONFIRMED)
                .build();


        Category categoryAdm = Category.builder()
                .name("testAdm")
                .build();
        categoryAdm = categoryRepository.save(categoryAdm);
        User userAdm = User.builder()
                .email("email@userAdm.test")
                .name("test")
                .build();
        userAdm = userRepository.save(userAdm);
        User user2Adm = User.builder()
                .email("email@user2Adm.test")
                .name("test2")
                .build();
        user2Adm = userRepository.save(user2Adm);
        Location locationAdm = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        locationAdm = locationRepository.save(locationAdm);
        event1Adm = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryAdm)
                .initiator(userAdm)
                .location(locationAdm)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test")
                .description("description")
                .annotation("annotation")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event1Adm = eventRepository.save(event1Adm);
        event2Adm = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryAdm)
                .initiator(user2Adm)
                .location(locationAdm)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test2")
                .description("description2")
                .annotation("annotation2")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event2Adm = eventRepository.save(event2Adm);
        event3NotPublished = Event.builder()
                .state(EventState.PENDING)
                .category(categoryAdm)
                .initiator(userAdm)
                .location(locationAdm)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test3")
                .description("description3")
                .annotation("annotation3")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event3NotPublished = eventRepository.save(event3NotPublished);
        adminEventParams = AdminEventParams.builder()
                .from(0)
                .size(10)
                .categories(List.of(categoryAdm.getId()))
                .users(List.of(userAdm.getId()))
                .build();
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event1Adm)
                .status(ParticipationRequestStatus.CONFIRMED)
                .requester(userAdm)
                .build();
        requestRepository.save(request);
        adminRequestPublish = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();
        adminRequestCancel = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.REJECT_EVENT)
                .build();


        Category categoryPub = Category.builder()
                .name("testPub")
                .build();
        categoryPub = categoryRepository.save(categoryPub);
        User userPub = User.builder()
                .email("email@userPub.test")
                .name("test")
                .build();
        userPub = userRepository.save(userPub);
        Location locationPub = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        locationPub = locationRepository.save(locationPub);
        event1Pub = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryPub)
                .initiator(userPub)
                .location(locationPub)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test")
                .description("description")
                .annotation("annotation")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event1Pub = eventRepository.save(event1Pub);
        event2Pub = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryPub)
                .initiator(userPub)
                .location(locationPub)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test2")
                .description("description2")
                .annotation("annotation2")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event2Pub = eventRepository.save(event2Pub);
        event3NotPublishedPub = Event.builder()
                .state(EventState.PENDING)
                .category(categoryPub)
                .initiator(userPub)
                .location(locationPub)
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .title("test3")
                .description("description3")
                .annotation("annotation3")
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusHours(1))
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();
        event3NotPublishedPub = eventRepository.save(event3NotPublishedPub);
        publicEventParams = PublicEventParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("desc")
                .onlyAvailable(false)
                .categories(List.of(categoryPub.getId()))
                .paid(false)
                .build();
        ParticipationRequest requestPub = ParticipationRequest.builder()
                .event(event1Pub)
                .status(ParticipationRequestStatus.CONFIRMED)
                .requester(userPub)
                .build();
        requestRepository.save(requestPub);
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventShortDtos() {
        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(user.getId(), page);

        assertThat(actualListOfEventShortDtos.size(), is(3));
        assertThat(actualListOfEventShortDtos.get(0).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(1).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(2).getId(), notNullValue());
    }

    @Test
    public void getAll_whenNoEventsFound_thenReturnEmptyList() {
        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(user2.getId(), page);

        assertThat(actualListOfEventShortDtos.size(), is(0));
    }

    @Test
    public void getAll_whenUserNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getAll(wrongId, page));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void add_whenSuccessful_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.add(user2.getId(), newEventDto);

        assertThat(actualEventFullDto.getId(), notNullValue());
        assertThat(actualEventFullDto.getInitiator().getId(), is(user2.getId()));
        assertThat(actualEventFullDto.getCategory().getId(), is(category.getId()));
        assertThat(actualEventFullDto.getAnnotation(), is(newEventDto.getAnnotation()));
        assertThat(actualEventFullDto.getDescription(), is(newEventDto.getDescription()));
        assertThat(actualEventFullDto.getTitle(), is(newEventDto.getTitle()));
        assertThat(actualEventFullDto.getState(), is(EventState.PENDING));
    }

    @Test
    public void add_whenUserNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.add(wrongId, newEventDto));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void add_whenCategoryNotFound_thenThrownException() {
        newEventDto.setCategory(wrongId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.add(user2.getId(), newEventDto));
        assertThat(exception.getMessage(), is("Category with id=" + wrongId + " was not found"));
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.get(user.getId(), event1.getId());

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event1.getId()));
        assertThat(actualEventFullDto.getInitiator().getId(), is(user.getId()));
    }

    @Test
    public void get_whenUserNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, event1.getId()));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void get_whenEventNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(user.getId(), wrongId));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
    }

    @Test
    public void update_whenSuccessful_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService
                .update(user.getId(), event1.getId(), updateEventUserRequest);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event1.getId()));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
    }

    @Test
    public void update_whenEventPublished_thenThrownException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.update(user.getId(), event2.getId(), updateEventUserRequest));
        assertThat(exception.getMessage(), is("Only pending or canceled events can be changed"));
    }

    @Test
    public void update_whenSendToReviewPendingEvent_thenReturnSameEventFullDto() {
        updateEventUserRequest.setStateAction(StateUserAction.SEND_TO_REVIEW);

        EventFullDto actualEventFullDto = eventService
                .update(user.getId(), event1.getId(), updateEventUserRequest);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event1.getId()));
        assertThat(actualEventFullDto.getState(), is(EventState.PENDING));
    }

    @Test
    public void update_whenCancelCanceledEvent_thenReturnSameEventFullDto() {
        updateEventUserRequest.setStateAction(StateUserAction.CANCEL_REVIEW);

        EventFullDto actualEventFullDto = eventService.update(user.getId(), event3.getId(), updateEventUserRequest);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event3.getId()));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
    }

    @Test
    public void update_whenUpdateToNotExistingCategory_thenThrownException() {
        updateEventUserRequest.setCategory(wrongId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.update(user.getId(), event1.getId(), updateEventUserRequest));
        assertThat(exception.getMessage(), is("Category with id=" +
                wrongId + " was not found"));
    }

    @Test
    public void getRequests_whenSuccessful_thenReturnListOfParticipationRequestDtos() {
        List<ParticipationRequestDto> actualListOfParticipationRequestDtos = eventService
                .getRequests(user.getId(), event1.getId());

        assertThat(actualListOfParticipationRequestDtos.size(), is(2));
        assertThat(actualListOfParticipationRequestDtos.get(0).getId(), is(participationRequest1.getId()));
        assertThat(actualListOfParticipationRequestDtos.get(1).getId(), is(participationRequest2.getId()));
    }

    @Test
    public void getRequests_whenUserNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getRequests(wrongId, event1.getId()));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void getRequests_whenEventNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getRequests(user.getId(), wrongId));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
    }

    @Test
    public void updateRequests_whenSuccessfulConfirm_thenReturnEventRequestStatusUpdateResult() {
        EventRequestStatusUpdateResult actualUpdateResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateRequest);

        assertThat(actualUpdateResult, notNullValue());
        assertThat(actualUpdateResult.getConfirmedRequests().size(), is(2));
        assertThat(actualUpdateResult.getRejectedRequests(), nullValue());
        assertThat(actualUpdateResult.getConfirmedRequests().get(0).getId(),
                is(participationRequest1.getId()));
        assertThat(actualUpdateResult.getConfirmedRequests().get(1).getId(),
                is(participationRequest2.getId()));
    }

    @Test
    public void updateRequests_whenSuccessfulReject_thenReturnEventRequestStatusUpdateResult() {
        updateRequest.setStatus(RequestUpdateAction.REJECTED);

        EventRequestStatusUpdateResult actualUpdateResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateRequest);

        assertThat(actualUpdateResult, notNullValue());
        assertThat(actualUpdateResult.getConfirmedRequests(), nullValue());
        assertThat(actualUpdateResult.getRejectedRequests().size(), is(2));
        assertThat(actualUpdateResult.getRejectedRequests().get(0).getId(),
                is(participationRequest1.getId()));
        assertThat(actualUpdateResult.getRejectedRequests().get(1).getId(),
                is(participationRequest2.getId()));
    }

    @Test
    public void updateRequests_whenLimitOfParticipantReached_thenReturnEventRequestStatusUpdateResult() {
        event1.setParticipantLimit(1);
        eventRepository.save(event1);

        EventRequestStatusUpdateResult actualUpdateResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateRequest);

        assertThat(actualUpdateResult, notNullValue());
        assertThat(actualUpdateResult.getConfirmedRequests().size(), is(1));
        assertThat(actualUpdateResult.getRejectedRequests().size(), is(1));
        assertThat(actualUpdateResult.getConfirmedRequests().get(0).getId(), is(participationRequest1.getId()));
        assertThat(actualUpdateResult.getRejectedRequests().get(0).getId(), is(participationRequest2.getId()));
    }

    @Test
    public void updateRequests_whenRequestsStatusOtherThanPending_thenThrownException() {
        participationRequest1.setStatus(ParticipationRequestStatus.CONFIRMED);
        requestRepository.save(participationRequest1);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateRequests(user.getId(), event1.getId(), updateRequest));
        assertThat(exception.getMessage(), is("Not all requests are in PENDING status"));
    }

    @Test
    public void updateRequests_whenUserNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateRequests(wrongId, event1.getId(), updateRequest));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void updateRequests_whenEventNotFound_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.updateRequests(user.getId(), wrongId, updateRequest));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId
                + " and initiator id=" + user.getId() + " was not found"));
    }


    @Test
    public void adm_getAll_whenSuccessful_thenReturnListOfEventFullDto() {
        List<EventFullDto> actualListOfEventFullDtos = eventService.getAll(adminEventParams);

        assertThat(actualListOfEventFullDtos, notNullValue());
        assertThat(actualListOfEventFullDtos.size(), is(2));
        assertThat(actualListOfEventFullDtos.get(0).getClass(), is(EventFullDto.class));
        assertThat(actualListOfEventFullDtos.get(0).getId(), is(event1Adm.getId()));
        assertThat(actualListOfEventFullDtos.get(1).getClass(), is(EventFullDto.class));
        assertThat(actualListOfEventFullDtos.get(1).getId(), is(event3NotPublished.getId()));
    }

    @Test
    public void adm_getAll_whenNoneEventFound_thenReturnEmptyList() {
        adminEventParams.setFrom(5);

        List<EventFullDto> actualListOfEventFullDtos = eventService.getAll(adminEventParams);

        assertThat(actualListOfEventFullDtos, notNullValue());
        assertThat(actualListOfEventFullDtos.size(), is(0));
    }

    @Test
    public void adm_patch_whenReject_thenReturnCanceledEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestCancel);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
    }

    @Test
    public void adm_patch_whenPublish_thenReturnPublishedEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestPublish);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(actualEventFullDto.getState(), is(EventState.PUBLISHED));
    }

    @Test
    public void adm_patch_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.patch(wrongId, adminRequestCancel));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
    }

    @Test
    public void adm_patch_whenCategoryToUpdateNotFound_thenThrownException() {
        long wrongCategoryId = 66L;
        adminRequestCancel.setCategory(wrongCategoryId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.patch(event3NotPublished.getId(), adminRequestCancel));
        assertThat(exception.getMessage(),
                is("Category with id=" + wrongCategoryId + " was not found"));
    }

    @Test
    public void adm_patch_whenPublishNotPendingEvent_thenThrownException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event1Adm.getId(), adminRequestPublish));
        assertThat(exception.getMessage(), is("Cannot publish the event because it's not in the right state: "
                + event1Adm.getState()));
    }

    @Test
    public void adm_patch_whenRejectNotPublishedEvent_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestCancel);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event3NotPublished.getId()));
    }


    @Test
    public void pub_getAll_whenSuccessful_thenReturnListOfEventShortDto() {
        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(publicEventParams, ipAddress, uri);

        assertThat(actualListOfEventShortDtos.size(), is(2));
        assertThat(actualListOfEventShortDtos.get(0).getClass(), is(EventShortDto.class));
        assertThat(actualListOfEventShortDtos.get(1).getClass(), is(EventShortDto.class));
        assertThat(actualListOfEventShortDtos, notNullValue());
        assertThat(actualListOfEventShortDtos.get(0).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(1).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(0).getTitle(), is(event1Pub.getTitle()));
        assertThat(actualListOfEventShortDtos.get(1).getTitle(), is(event2Pub.getTitle()));
    }

    @Test
    public void pub_getAll_whenNoneEventFound_thenReturnEmptyList() {
        publicEventParams.setFrom(3);

        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(publicEventParams, ipAddress, uri);

        assertThat(actualListOfEventShortDtos, emptyIterable());
        assertThat(actualListOfEventShortDtos, notNullValue());
    }

    @Test
    public void pub_get_whenSuccessful_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.get(event1Pub.getId(), ipAddress, uri);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(event1Pub.getId(), is(actualEventFullDto.getId()));
        assertThat(actualEventFullDto.getConfirmedRequests(), is(1L));
    }

    @Test
    public void pub_get_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, ipAddress, uri));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
    }

    @Test
    public void pub_get_whenEventNotPublished_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(event3NotPublishedPub.getId(), ipAddress, uri));
        assertThat(exception.getMessage(),
                is("Event with id=" + event3NotPublishedPub.getId() + " was not found"));
    }

    @Test
    public void like_whenSuccessful_thenReturnEventFullDtoWithRating() {
        EventFullDto actualEventFullDto = eventService.like(user2.getId(), event2.getId());

        assertThat(actualEventFullDto.getRating(), is(1L));
    }

    @Test
    public void like_whenSuccessful_thenInitiatorRatingIncreased() {
        eventService.like(user2.getId(), event2.getId());

        List<UserDto> users = userService.getAll(List.of(user.getId()), pageUsers);

        assertThat(users.get(0).getRating(), is(1L));
    }

    @Test
    public void dislike_whenSuccessful_thenInitiatorRatingDecreased() {
        eventService.dislike(user2.getId(), event2.getId());

        List<UserDto> users = userService.getAll(List.of(user.getId()), pageUsers);

        assertThat(users.get(0).getRating(), is(-1L));
    }

    @Test
    public void removeLike_whenSuccessful_thenInitiatorRatingDecreased() {
        eventService.like(user2.getId(), event2.getId());
        eventService.removeLike(user2.getId(), event2.getId());

        List<UserDto> users = userService.getAll(List.of(user.getId()), pageUsers);

        assertThat(users.get(0).getRating(), is(0L));
    }

    @Test
    public void removeDislike_whenSuccessful_thenInitiatorRatingIncreased() {
        eventService.dislike(user2.getId(), event2.getId());
        eventService.removeDislike(user2.getId(), event2.getId());

        List<UserDto> users = userService.getAll(List.of(user.getId()), pageUsers);

        assertThat(users.get(0).getRating(), is(0L));
    }

    @Test
    public void like_dislike_whenSuccessful_thenInitiatorRatingCorrect() {
        eventService.like(user2.getId(), event2.getId());
        eventService.like(user33.getId(), event2.getId());
        eventService.dislike(user4.getId(), event2.getId());

        List<UserDto> users = userService.getAll(List.of(user.getId()), pageUsers);

        assertThat(users.get(0).getRating(), is(1L));
    }

    @Test
    public void when_sortByRating_thenReturnListOfEventDtoSortedByRating() {
        publicEventParams.setSort("RATING");
        publicEventParams.setText(null);
        publicEventParams.setCategories(null);
        eventService.patch(event1.getId(), adminRequestPublish);
        eventService.like(user2.getId(), event1.getId());
        eventService.like(user2.getId(), event2.getId());
        eventService.like(user33.getId(), event1.getId());

        List<EventShortDto> events = eventService.getAll(publicEventParams, ipAddress, uri);

        assertThat(events.size(), is(6));
        assertThat(events.get(0).getRating(), is(2L));
        assertThat(events.get(1).getRating(), is(1L));
    }
}
