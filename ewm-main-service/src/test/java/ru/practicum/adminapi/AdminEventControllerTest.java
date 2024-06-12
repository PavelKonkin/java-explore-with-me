package ru.practicum.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindException;
import ru.practicum.event.EventQueryParams;
import ru.practicum.event.EventState;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.StateAdminAction;
import ru.practicum.event.dto.UpdateEventAdminRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = AdminEventController.class)
public class AdminEventControllerTest {
    @MockBean
    private AdminEventService eventService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;


    private EventFullDto eventFullDto1;
    private EventFullDto eventFullDto2;
    private EventFullDto eventFullDto2Updated;
    private EventQueryParams eventQueryParams;
    private UpdateEventAdminRequest adminRequest;

    @BeforeEach
    public void setup() {
        eventFullDto1 = EventFullDto.builder()
                .id(1L)
                .state(EventState.PENDING)
                .build();
        eventFullDto2 = EventFullDto.builder()
                .id(2L)
                .state(EventState.PENDING)
                .build();
        eventFullDto2Updated = EventFullDto.builder()
                .id(2L)
                .state(EventState.PUBLISHED)
                .build();
        eventQueryParams = EventQueryParams.builder()
                .from(0)
                .size(10)
                .categories(List.of())
                .users(List.of())
                .states(List.of())
                .sort("EVENT_DATE")
                .onlyAvailable(false)
                .build();
        adminRequest = UpdateEventAdminRequest.builder()
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventFullDtos() throws Exception {
        when(eventService.getAll(eventQueryParams)).thenReturn(List.of(eventFullDto1, eventFullDto2));

        mvc.perform(get("/admin/events")
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(eventFullDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(eventFullDto2.getId()), Long.class));
        verify(eventService, times(1)).getAll(eventQueryParams);
    }

    @Test
    public void getAll_whenRequestParamNotValid_thenThrownException() throws Exception {
        mvc.perform(get("/admin/events?from=ad")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));
        verify(eventService, never()).getAll(eventQueryParams);
    }

    @Test
    public void patch_whenSuccessful_thenReturnUpdatedEventFullDto() throws Exception {
        when(eventService.patch(eventFullDto2.getId(), adminRequest)).thenReturn(eventFullDto2Updated);

        mvc.perform(patch("/admin/events/" + eventFullDto2.getId())
                        .content(mapper.writeValueAsString(adminRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto2.getId()), Long.class))
                .andExpect(jsonPath("$.state", is(EventState.PUBLISHED.name())));
        verify(eventService, times(1)).patch(eventFullDto2.getId(), adminRequest);
    }

    @Test
    public void patch_whenEventDateNotLaterThenPublishDate_thenThrownException() throws Exception {
        eventFullDto2.setPublishedOn(LocalDateTime.now());
        adminRequest.setEventDate(LocalDateTime.now());

        mvc.perform(patch("/admin/events/" + eventFullDto2.getId())
                        .content(mapper.writeValueAsString(adminRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));

        verify(eventService, never()).patch(eventFullDto2.getId(), adminRequest);
    }
}
