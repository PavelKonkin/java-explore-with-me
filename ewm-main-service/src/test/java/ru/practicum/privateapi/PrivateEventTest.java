package ru.practicum.privateapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
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

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class PrivateEventTest {
    @Autowired
    private PrivateEventService eventService;
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

    private final Sort sort = Sort.by("createdOn").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);
    private final long wrongId = 66L;
    private User user;
    private User user2;
    private Category category;
    private NewEventDto newEventDto;
    private Event event1;
    private Event event2;
    private Event event3;
    private UpdateEventUserRequest updateEventUserRequest;
    private ParticipationRequest participationRequest1;
    private ParticipationRequest participationRequest2;
    private EventRequestStatusUpdateRequest updateRequest;

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
        EventFullDto actualEventFullDto = eventService.update(user.getId(), event1.getId(), updateEventUserRequest);

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

        EventFullDto actualEventFullDto = eventService.update(user.getId(), event1.getId(), updateEventUserRequest);

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
        assertThat(actualUpdateResult.getConfirmedRequests().get(0).getId(), is(participationRequest1.getId()));
        assertThat(actualUpdateResult.getConfirmedRequests().get(1).getId(), is(participationRequest2.getId()));
    }

    @Test
    public void updateRequests_whenSuccessfulReject_thenReturnEventRequestStatusUpdateResult() {
        updateRequest.setStatus(RequestUpdateAction.REJECTED);

        EventRequestStatusUpdateResult actualUpdateResult = eventService
                .updateRequests(user.getId(), event1.getId(), updateRequest);

        assertThat(actualUpdateResult, notNullValue());
        assertThat(actualUpdateResult.getConfirmedRequests(), nullValue());
        assertThat(actualUpdateResult.getRejectedRequests().size(), is(2));
        assertThat(actualUpdateResult.getRejectedRequests().get(0).getId(), is(participationRequest1.getId()));
        assertThat(actualUpdateResult.getRejectedRequests().get(1).getId(), is(participationRequest2.getId()));
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

}
