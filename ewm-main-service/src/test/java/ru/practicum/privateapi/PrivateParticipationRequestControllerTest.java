package ru.practicum.privateapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MissingServletRequestParameterException;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateParticipationRequestController.class)
public class PrivateParticipationRequestControllerTest {
    @MockBean
    private PrivateParticipationRequestService participationRequestService;
    @Autowired
    private MockMvc mvc;

    private ParticipationRequestDto participationRequestDto;
    private ParticipationRequestDto participationRequestCanceledDto;
    private final long userId = 1L;
    private final long eventId = 1L;
    private long requestId = 1L;
    private final LocalDateTime created = LocalDateTime.now();

    @BeforeEach
    public void setup() {
        participationRequestDto = ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .event(eventId)
                .created(created)
                .status(ParticipationRequestStatus.PENDING.name())
                .build();
        participationRequestCanceledDto = ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .event(eventId)
                .created(created)
                .status(ParticipationRequestStatus.REJECTED.name())
                .build();
    }

    @Test
    public void add_whenSuccessful_thenReturnParticipationRequestDto() throws Exception {
        when(participationRequestService.add(any(Long.class), any(Long.class), any(LocalDateTime.class)))
                .thenReturn(participationRequestDto);

        mvc.perform(post("/users/" + userId + "/requests?eventId=" + eventId)
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(participationRequestDto.getId()), Long.class))
                .andExpect(jsonPath("$.requester",
                        is(participationRequestDto.getRequester()), Long.class))
                .andExpect(jsonPath("$.event", is(participationRequestDto.getEvent()), Long.class))
                .andExpect(jsonPath("$.created",
                        is(participationRequestDto.getCreated()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
                .andExpect(jsonPath("$.status", is(participationRequestDto.getStatus())));
        verify(participationRequestService, times(1))
                .add(any(Long.class), any(Long.class), any(LocalDateTime.class));
    }

    @Test
    public void add_whenPathVariableMissing_thenThrownException() throws Exception {
        mvc.perform(post("/users/" + userId + "/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MissingServletRequestParameterException.class,
                        result.getResolvedException()));;
        verify(participationRequestService, never())
                .add(any(Long.class), any(Long.class), any(LocalDateTime.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnListOfParticipationRequestDto() throws Exception {
        when(participationRequestService.get(userId))
                .thenReturn(List.of(participationRequestDto));

        mvc.perform(get("/users/" + userId + "/requests")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(participationRequestDto.getId()), Long.class))
                .andExpect(jsonPath("$.[0].requester",
                        is(participationRequestDto.getRequester()), Long.class))
                .andExpect(jsonPath("$.[0].event", is(participationRequestDto.getEvent()), Long.class))
                .andExpect(jsonPath("$.[0].created",
                        is(participationRequestDto.getCreated()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
                .andExpect(jsonPath("$.[0].status", is(participationRequestDto.getStatus())));
        verify(participationRequestService, times(1)).get(userId);
    }

    @Test
    public void cancel_whenSuccessful_thenReturnCanceledParticipationRequestDto() throws Exception {
        when(participationRequestService.cancel(userId, requestId)).thenReturn(participationRequestCanceledDto);

        mvc.perform(patch("/users/" + userId + "/requests/" + requestId + "/cancel")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(participationRequestCanceledDto.getId()), Long.class))
                .andExpect(jsonPath("$.requester",
                        is(participationRequestCanceledDto.getRequester()), Long.class))
                .andExpect(jsonPath("$.event", is(participationRequestCanceledDto.getEvent()), Long.class))
                .andExpect(jsonPath("$.created",
                        is(participationRequestCanceledDto.getCreated()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
                .andExpect(jsonPath("$.status", is(participationRequestCanceledDto.getStatus())));
        verify(participationRequestService, times(1))
                .cancel(userId, requestId);
    }
}
