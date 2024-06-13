package ru.practicum.adminapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.StateAdminAction;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
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
public class AdminEventTest {
    @Autowired
    private AdminEventService eventService;
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

    private Event event1;
    private Event event2;
    private Event event3NotPublished;
    private EventQueryParams eventQueryParams;
    private UpdateEventAdminRequest adminRequestPublish;
    private UpdateEventAdminRequest adminRequestCancel;

    @BeforeEach
    public void setup() {
        Category category = Category.builder()
                .name("test")
                .build();
        category = categoryRepository.save(category);
        User user = User.builder()
                .email("email@user.test")
                .name("test")
                .build();
        user = userRepository.save(user);
        User user2 = User.builder()
                .email("email@user2.test")
                .name("test2")
                .build();
        user2 = userRepository.save(user2);
        Location location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        event1 = Event.builder()
                .state(EventState.PUBLISHED)
                .category(category)
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
                .state(EventState.PUBLISHED)
                .category(category)
                .initiator(user2)
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
        event3NotPublished = Event.builder()
                .state(EventState.PENDING)
                .category(category)
                .initiator(user)
                .location(location)
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
        eventQueryParams = EventQueryParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("desc")
                .onlyAvailable(false)
                .categories(List.of(category.getId()))
                .paid(false)
                .users(List.of(user.getId()))
                .build();
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event1)
                .status(ParticipationRequestStatus.CONFIRMED)
                .requester(user)
                .build();
        requestRepository.save(request);
        adminRequestPublish = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();
        adminRequestCancel = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.REJECT_EVENT)
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventFullDto() {
        List<EventFullDto> actualListOfEventFullDtos = eventService.getAll(eventQueryParams);

        assertThat(actualListOfEventFullDtos, notNullValue());
        assertThat(actualListOfEventFullDtos.size(), is(2));
        assertThat(actualListOfEventFullDtos.get(0).getClass(), is(EventFullDto.class));
        assertThat(actualListOfEventFullDtos.get(0).getId(), is(event1.getId()));
        assertThat(actualListOfEventFullDtos.get(1).getClass(), is(EventFullDto.class));
        assertThat(actualListOfEventFullDtos.get(1).getId(), is(event3NotPublished.getId()));
    }

    @Test
    public void getAll_whenNoneEventFound_thenReturnEmptyList() {
        eventQueryParams.setFrom(5);

        List<EventFullDto> actualListOfEventFullDtos = eventService.getAll(eventQueryParams);

        assertThat(actualListOfEventFullDtos, notNullValue());
        assertThat(actualListOfEventFullDtos.size(), is(0));
    }

    @Test
    public void patch_whenReject_thenReturnCanceledEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestCancel);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
    }

    @Test
    public void patch_whenPublish_thenReturnPublishedEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestPublish);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(actualEventFullDto.getState(), is(EventState.PUBLISHED));
    }

    @Test
    public void patch_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.patch(wrongId, adminRequestCancel));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
    }

    @Test
    public void patch_whenCategoryToUpdateNotFound_thenThrownException() {
        long wrongCategoryId = 66L;
        adminRequestCancel.setCategory(wrongCategoryId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.patch(event3NotPublished.getId(), adminRequestCancel));
        assertThat(exception.getMessage(), is("Category with id=" + wrongCategoryId + " was not found"));
    }

    @Test
    public void patch_whenPublishNotPendingEvent_thenThrownException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event1.getId(), adminRequestPublish));
        assertThat(exception.getMessage(), is("Cannot publish the event because it's not in the right state: "
                + event1.getState()));
    }

    @Test
    public void patch_whenRejectNotPublishedEvent_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.patch(event3NotPublished.getId(), adminRequestCancel);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getId(), is(event3NotPublished.getId()));
    }

}
