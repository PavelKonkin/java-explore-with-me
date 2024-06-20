package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
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
public class EventTest {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ParticipationRequestRepository requestRepository;
    @Autowired
    private EventService eventService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LocationRepository locationRepository;

    private Event event1;
    private Event event2;
    private Event event3NotPublished;
    private PublicEventParams params;
    private final String uri = "/events";
    private final String ipAddress = "111.0.0.0";


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
        params = PublicEventParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("desc")
                .onlyAvailable(false)
                .categories(List.of(category.getId()))
                .paid(false)
                .build();
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event1)
                .status(ParticipationRequestStatus.CONFIRMED)
                .requester(user)
                .build();
        requestRepository.save(request);
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventShortDto() {
        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(params, ipAddress, uri);

        assertThat(actualListOfEventShortDtos.size(), is(2));
        assertThat(actualListOfEventShortDtos.get(0).getClass(), is(EventShortDto.class));
        assertThat(actualListOfEventShortDtos.get(1).getClass(), is(EventShortDto.class));
        assertThat(actualListOfEventShortDtos, notNullValue());
        assertThat(actualListOfEventShortDtos.get(0).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(1).getId(), notNullValue());
        assertThat(actualListOfEventShortDtos.get(0).getTitle(), is(event1.getTitle()));
        assertThat(actualListOfEventShortDtos.get(1).getTitle(), is(event2.getTitle()));
    }

    @Test
    public void getAll_whenNoneEventFound_thenReturnEmptyList() {
        params.setFrom(3);

        List<EventShortDto> actualListOfEventShortDtos = eventService.getAll(params, ipAddress, uri);

        assertThat(actualListOfEventShortDtos, emptyIterable());
        assertThat(actualListOfEventShortDtos, notNullValue());
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() {
        EventFullDto actualEventFullDto = eventService.get(event1.getId(), ipAddress, uri);

        assertThat(actualEventFullDto, notNullValue());
        assertThat(actualEventFullDto.getClass(), is(EventFullDto.class));
        assertThat(event1.getId(), is(actualEventFullDto.getId()));
        assertThat(actualEventFullDto.getConfirmedRequests(), is(1L));
    }

    @Test
    public void get_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, ipAddress, uri));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
    }

    @Test
    public void get_whenEventNotPublished_thenThrownException() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(event3NotPublished.getId(), ipAddress, uri));
        assertThat(exception.getMessage(), is("Event with id=" + event3NotPublished.getId() + " was not found"));
    }
}
