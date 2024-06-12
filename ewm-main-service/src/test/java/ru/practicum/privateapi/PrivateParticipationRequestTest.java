package ru.practicum.privateapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestMapper;
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
public class PrivateParticipationRequestTest {
    @Autowired
    private ParticipationRequestRepository participationRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ParticipationRequestMapper participationRequestMapper;
    @Autowired
    private PrivateParticipationRequestService participationRequestService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private LocationRepository locationRepository;

    private User eventInitiator;
    private User requester;
    private User requester2;
    private User requester3;
    private Event event;
    private Event notPublishedEvent;
    private ParticipationRequest participationRequest;

    @BeforeEach
    public void setup() {
        Category categoryWithEvent = Category.builder()
                .name("category with event")
                .build();
        categoryWithEvent = categoryRepository.save(categoryWithEvent);
        Location location = Location.builder()
                .lat(22.22f)
                .lon(33.33f)
                .build();
        location = locationRepository.save(location);
        eventInitiator = User.builder()
                .name("test user")
                .email("eventInitiator@email.user")
                .build();
        eventInitiator = userRepository.save(eventInitiator);
        event = Event.builder()
                .state(EventState.PUBLISHED)
                .category(categoryWithEvent)
                .location(location)
                .paid(false)
                .participantLimit(2)
                .initiator(eventInitiator)
                .requestModeration(false)
                .title("test")
                .description("test")
                .annotation("test")
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .publishedOn(LocalDateTime.now().plusHours(2))
                .build();
        eventRepository.save(event);
        notPublishedEvent = event.toBuilder()
                .id(null)
                .state(EventState.PENDING)
                .build();
        eventRepository.save(notPublishedEvent);
        requester = User.builder()
                .name("test user")
                .email("requester@email.user")
                .build();
        requester = userRepository.save(requester);
        participationRequest = ParticipationRequest.builder()
                .status(ParticipationRequestStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .requester(requester)
                .event(event)
                .build();
        participationRequest = participationRequestRepository.save(participationRequest);
        requester2 = User.builder()
                .name("requester2")
                .email("requester2@email.test")
                .build();
        requester2 = userRepository.save(requester2);
        requester3 = User.builder()
                .name("requester3")
                .email("requester3@email.test")
                .build();
        requester3 = userRepository.save(requester3);
    }

    @Test
    public void add_whenSuccessful_thenReturnParticipationRequestDto() {
        ParticipationRequestDto actualResult = participationRequestService
                .add(requester2.getId(), event.getId(), LocalDateTime.now());

        assertThat(actualResult.getClass(), is(ParticipationRequestDto.class));
        assertThat(actualResult.getId(), notNullValue());
        assertThat(actualResult.getRequesterId(), is(requester2.getId()));
        assertThat(actualResult.getEventId(), is(event.getId()));
    }

    @Test
    public void add_whenUserNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.add(wrongId, event.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void add_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.add(requester.getId(), wrongId, LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
    }

    @Test
    public void add_whenInitiatorIsRequester_thenThrownException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationRequestService.add(eventInitiator.getId(), event.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Initiator of event cannot request participation in it"));
    }

    @Test
    public void add_whenRequestToUnpublishedEvent_thenThrownException() {
        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationRequestService.add(requester.getId(),
                        notPublishedEvent.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Attempt of request participation in not published event"));
    }

    @Test
    public void add_whenLimitOfParticipantExceeded_thenThrownException() {
        participationRequestService
                .add(requester2.getId(), event.getId(), LocalDateTime.now());

        ConflictException exception = assertThrows(ConflictException.class, () -> participationRequestService
                .add(requester3.getId(), event.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Limit of participants of event is exceeded"));
    }

    @Test
    public void get_whenSuccessful_thenReturnListOfParticipationRequestDto() {
        List<ParticipationRequestDto> actualResult = participationRequestService.get(requester.getId());

        assertThat(actualResult,
                contains(participationRequestMapper.convertParticipationRequest(participationRequest)));
    }

    @Test
    public void get_whenUserNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.get(wrongId));

        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void cancel_whenSuccessful_thenReturnCanceledParticipationRequestDto() {
        ParticipationRequestDto actualResult = participationRequestService
                .cancel(requester.getId(), participationRequest.getId());

        assertThat(actualResult.getStatus(), is(ParticipationRequestStatus.CANCELED.name()));
    }

    @Test
    public void cancel_whenUserNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () ->  participationRequestService
                .cancel(wrongId, participationRequest.getId()));

        assertThat(exception.getMessage(), is("User with id=" + wrongId + " was not found"));
    }

    @Test
    public void cancel_whenRequestNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class, () ->  participationRequestService
                .cancel(requester.getId(), wrongId));

        assertThat(exception.getMessage(), is("Request with id=" + wrongId + " was not found"));
    }


}
