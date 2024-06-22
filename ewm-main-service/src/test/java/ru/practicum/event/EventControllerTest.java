package ru.practicum.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.event.dto.*;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = EventController.class)
public class EventControllerTest {
    @MockBean
    private EventService eventService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventFullDto eventFullDto;
    private NewEventDto newEventDto;
    private UpdateEventUserRequest updateEventUserRequest;
    private ParticipationRequestDto participationRequestDto1;
    private ParticipationRequestDto participationRequestDto2;
    private EventRequestStatusUpdateRequest requestStatusUpdateRequest;
    private EventRequestStatusUpdateResult requestStatusUpdateResult;
    private final Sort sort = Sort.by("createdOn").descending();
    private final Pageable page = new OffsetPage(0, 10, sort);
    private final long userId = 1L;
    private final String wrongId = "ad";
    private final long eventId = 1L;


    private EventFullDto eventFullDto1Adm;
    private EventFullDto eventFullDto2Adm;
    private EventFullDto eventFullDto2UpdatedAdm;
    private AdminEventParams adminEventParams;
    private UpdateEventAdminRequest adminRequest;


    private EventShortDto eventShortDto1Pub;
    private EventShortDto eventShortDto2Pub;
    private EventFullDto eventFullDtoPub;
    private PublicEventParams publicEventParams;
    private final String uri = "/events";
    private final String ipAddress = "127.0.0.1";

    @BeforeEach
    public void setup() {
        eventShortDto1 = EventShortDto.builder()
                .id(1L)
                .build();
        eventShortDto2 = EventShortDto.builder()
                .id(2L)
                .build();
        eventFullDto = EventFullDto.builder()
                .id(2L)
                .state(EventState.CANCELED)
                .build();
        LocationDto locationDto = LocationDto.builder()
                .lon(22.22f)
                .lat(33.33f)
                .build();
        newEventDto = NewEventDto.builder()
                .category(1L)
                .title("test")
                .eventDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusHours(5))
                .description("descriptiondescriptiondescription")
                .annotation("annotationannotationannotation")
                .location(locationDto)
                .build();
        updateEventUserRequest = UpdateEventUserRequest.builder()
                .stateAction(StateUserAction.CANCEL_REVIEW)
                .build();
        participationRequestDto1 = ParticipationRequestDto.builder()
                .id(1L)
                .build();
        participationRequestDto2 = ParticipationRequestDto.builder()
                .id(2L)
                .build();
        requestStatusUpdateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L, 2L))
                .status(RequestUpdateAction.CONFIRMED)
                .build();
        requestStatusUpdateResult = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(participationRequestDto1, participationRequestDto2))
                .build();


        eventFullDto1Adm = EventFullDto.builder()
                .id(1L)
                .state(EventState.PENDING)
                .build();
        eventFullDto2Adm = EventFullDto.builder()
                .id(2L)
                .state(EventState.PENDING)
                .build();
        eventFullDto2UpdatedAdm = EventFullDto.builder()
                .id(2L)
                .state(EventState.PUBLISHED)
                .build();
        adminEventParams = AdminEventParams.builder()
                .from(0)
                .size(10)
                .categories(List.of())
                .users(List.of())
                .states(List.of())
                .build();
        adminRequest = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();


        eventShortDto1Pub = EventShortDto.builder()
                .id(1L)
                .build();
        eventShortDto2Pub = EventShortDto.builder()
                .id(2L)
                .build();
        eventFullDtoPub = EventFullDto.builder()
                .id(3L)
                .build();
        publicEventParams = PublicEventParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("test")
                .onlyAvailable(false)
                .categories(List.of(1L))
                .paid(false)
                .users(List.of())
                .states(List.of())
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventShortDtos() throws Exception {
        when(eventService.getAll(userId, page)).thenReturn(List.of(eventShortDto1, eventShortDto2));

        mvc.perform(get("/users/" + userId + "/events")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(eventShortDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(eventShortDto2.getId()), Long.class));
        verify(eventService, times(1)).getAll(userId, page);
    }

    @Test
    public void getAll_whenNotValidPathVariable_thenThrownException() throws Exception {
        mvc.perform(get("/users/" + wrongId + "/events")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never()).getAll(userId, page);
    }

    @Test
    public void post_whenSuccessful_thenReturnEventFullDto() throws Exception {
        when(eventService.add(userId, newEventDto)).thenReturn(eventFullDto);

        mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Long.class));
        verify(eventService, times(1)).add(userId, newEventDto);
    }

    @Test
    public void post_whenEventDateSoonerThan2Hours_thenThrownException() throws Exception {
        newEventDto.setEventDate(LocalDateTime.now().plusHours(1));

        mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).add(userId, newEventDto);
    }

    @Test
    public void post_whenTitleIsNull_thenThrownException() throws Exception {
        newEventDto.setTitle(null);

        mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).add(userId, newEventDto);
    }

    @Test
    public void post_whenTitleLengthIsLessThan3Symbols_thenThrownException() throws Exception {
        newEventDto.setTitle("1");

        mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).add(userId, newEventDto);
    }

    @Test
    public void post_whenTitleLengthIsMoreThan120Symbols_thenThrownException() throws Exception {
        newEventDto.setTitle(String.join("","1").repeat(121));

        mvc.perform(post("/users/" + userId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).add(userId, newEventDto);
    }

    @Test
    public void post_whenNotValidPathVariable_thenThrownException() throws Exception {
        mvc.perform(post("/users/" + wrongId + "/events")
                        .content(mapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never()).add(userId, newEventDto);
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() throws Exception {
        when(eventService.get(userId, eventId)).thenReturn(eventFullDto);

        mvc.perform(get("/users/" + userId + "/events/" + eventId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Long.class));
        verify(eventService, times(1)).get(userId, eventId);
    }

    @Test
    public void get_whenNotValidPathVariable_thenThrownException() throws Exception {
        mvc.perform(get("/users/" + wrongId + "/events/" + eventId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never()).get(userId, eventId);
    }

    @Test
    public void update_whenSuccessful_thenReturnEventFullDto() throws Exception {
        when(eventService.update(userId, eventId, updateEventUserRequest)).thenReturn(eventFullDto);

        mvc.perform(patch("/users/" + userId + "/events/" + eventId)
                        .content(mapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Long.class));
        verify(eventService, times(1)).update(userId, eventId, updateEventUserRequest);
    }

    @Test
    public void update_whenEventDateSoonerThan2Hours_thenThrownException() throws Exception {
        updateEventUserRequest.setEventDate(LocalDateTime.now().plusHours(1));

        mvc.perform(patch("/users/" + userId + "/events/" + eventId)
                        .content(mapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).update(userId, eventId, updateEventUserRequest);
    }

    @Test
    public void update_whenTitleLengthIsLessThan3Symbols_thenThrownException() throws Exception {
        updateEventUserRequest.setTitle("1");

        mvc.perform(patch("/users/" + userId + "/events/" + eventId)
                        .content(mapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).update(userId, eventId, updateEventUserRequest);
    }

    @Test
    public void update_whenTitleLengthIsMoreThan120Symbols_thenThrownException() throws Exception {
        updateEventUserRequest.setTitle(String.join("","1").repeat(121));

        mvc.perform(patch("/users/" + userId + "/events/" + eventId)
                        .content(mapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).update(userId, eventId, updateEventUserRequest);
    }

    @Test
    public void update_whenParticipantLimitNegative_thenThrownException() throws Exception {
        updateEventUserRequest.setParticipantLimit(-10);

        mvc.perform(patch("/users/" + userId + "/events/" + eventId)
                        .content(mapper.writeValueAsString(updateEventUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class,
                        result.getResolvedException()));
        verify(eventService, never()).update(userId, eventId, updateEventUserRequest);
    }

    @Test
    public void getRequests_whenSuccessful_thenReturnListOfParticipationRequestDtos() throws Exception {
        when(eventService.getRequests(userId, eventId))
                .thenReturn(List.of(participationRequestDto1, participationRequestDto2));

        mvc.perform(get("/users/" + userId + "/events/" + eventId + "/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(participationRequestDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(participationRequestDto2.getId()), Long.class));
        verify(eventService, times(1)).getRequests(userId, eventId);
    }

    @Test
    public void getRequests_whenNotValidPathVariable_thenThrownException() throws Exception {
        mvc.perform(get("/users/" + wrongId + "/events/" + eventId + "/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never()).getRequests(userId, eventId);
    }

    @Test
    public void updateRequests_whenSuccessful_thenReturnEventRequestStatusUpdateResult() throws Exception {
        when(eventService.updateRequests(userId, eventId, requestStatusUpdateRequest))
                .thenReturn(requestStatusUpdateResult);

        mvc.perform(patch("/users/" + userId + "/events/" + eventId + "/requests")
                        .content(mapper.writeValueAsString(requestStatusUpdateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.[0].id",
                        is(participationRequestDto1.getId()), Long.class))
                .andExpect(jsonPath("$.confirmedRequests.[1].id",
                        is(participationRequestDto2.getId()), Long.class));
        verify(eventService, times(1))
                .updateRequests(userId, eventId, requestStatusUpdateRequest);
    }

    @Test
    public void updateRequests_whenNotValidPathVariable_thenThrownException() throws Exception {
        mvc.perform(patch("/users/" + wrongId + "/events/" + eventId + "/requests")
                        .content(mapper.writeValueAsString(requestStatusUpdateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never()).updateRequests(userId, eventId, requestStatusUpdateRequest);
    }


    @Test
    public void adm_getAll_whenSuccessful_thenReturnListOfEventFullDtos() throws Exception {
        when(eventService.getAll(adminEventParams)).thenReturn(List.of(eventFullDto1Adm, eventFullDto2Adm));

        mvc.perform(get("/admin/events")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(eventFullDto1Adm.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(eventFullDto2Adm.getId()), Long.class));
        verify(eventService, times(1)).getAll(adminEventParams);
    }

    @Test
    public void adm_getAll_whenRequestParamNotValid_thenThrownException() throws Exception {
        mvc.perform(get("/admin/events?from=ad")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));
        verify(eventService, never()).getAll(adminEventParams);
    }

    @Test
    public void adm_patch_whenSuccessful_thenReturnUpdatedEventFullDto() throws Exception {
        when(eventService.patch(eventFullDto2Adm.getId(), adminRequest)).thenReturn(eventFullDto2UpdatedAdm);

        mvc.perform(patch("/admin/events/" + eventFullDto2Adm.getId())
                        .content(mapper.writeValueAsString(adminRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto2Adm.getId()), Long.class))
                .andExpect(jsonPath("$.state", is(EventState.PUBLISHED.name())));
        verify(eventService, times(1)).patch(eventFullDto2Adm.getId(), adminRequest);
    }

    @Test
    public void adm_patch_whenEventDateNotLaterThenPublishDate_thenThrownException() throws Exception {
        eventFullDto2Adm.setPublishedOn(LocalDateTime.now());
        adminRequest.setEventDate(LocalDateTime.now());

        mvc.perform(patch("/admin/events/" + eventFullDto2Adm.getId())
                        .content(mapper.writeValueAsString(adminRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));

        verify(eventService, never()).patch(eventFullDto2Adm.getId(), adminRequest);
    }


    @Test
    public void pub_getAll_whenSuccessful_thenReturnListOfEventShortDtos() throws Exception {
        when(eventService.getAll(publicEventParams, ipAddress, uri))
                .thenReturn(List.of(eventShortDto1Pub, eventShortDto2Pub));

        mvc.perform(get("/events?" + "from=" + publicEventParams.getFrom()
                        + "&size=" + publicEventParams.getSize() + "&sort=" + publicEventParams.getSort()
                        + "&categories=" + publicEventParams.getCategories().get(0) + "&text=" + publicEventParams.getText()
                        + "&onlyAvailable=" + publicEventParams.getOnlyAvailable() + "&paid=" + publicEventParams.getPaid())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(eventShortDto1Pub.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(eventShortDto2Pub.getId()), Long.class));
        verify(eventService, times(1)).getAll(publicEventParams, ipAddress, uri);
    }

    @Test
    public void pub_getAll_whenWrongSortQueryParam_thenThrownException() throws Exception {
        mvc.perform(get("/events?" + "from=" + publicEventParams.getFrom()
                        + "&size=" + publicEventParams.getSize() + "&sort=" + "hits"
                        + "&categories=" + publicEventParams.getCategories().get(0)
                        + "&text=" + publicEventParams.getText()
                        + "&onlyAvailable=" + publicEventParams.getOnlyAvailable()
                        + "&paid=" + publicEventParams.getPaid())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));
        verify(eventService, never()).getAll(publicEventParams, ipAddress, uri);
    }

    @Test
    public void pub_get_whenSuccessful_thenReturnEventFullDto() throws Exception {
        when(eventService.get(eventFullDtoPub.getId(), ipAddress, uri + "/" + eventFullDtoPub.getId()))
                .thenReturn(eventFullDtoPub);

        mvc.perform(get("/events/" + eventFullDtoPub.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDtoPub.getId()), Long.class));
        verify(eventService, times(1))
                .get(eventFullDtoPub.getId(), ipAddress, uri + "/" + eventFullDtoPub.getId());
    }

    @Test
    public void pub_get_whenNotValidPathVariable_thenThrownException() throws Exception {
        String wrongId = "ad";

        mvc.perform(get("/events/" + wrongId)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));
        verify(eventService, never())
                .get(any(Long.class), anyString(), anyString());
    }
}
