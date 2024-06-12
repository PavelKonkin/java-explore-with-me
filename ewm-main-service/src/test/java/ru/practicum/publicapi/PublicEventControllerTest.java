package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.event.EventQueryParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = PublicEventController.class)
public class PublicEventControllerTest {
    @MockBean
    private PublicEventService eventService;
    @Autowired
    private MockMvc mvc;

    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventFullDto eventFullDto;
    private EventQueryParams eventQueryParams;
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
                .id(3L)
                .build();
        eventQueryParams = EventQueryParams.builder()
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
        when(eventService.getAll(eventQueryParams, ipAddress, uri))
                .thenReturn(List.of(eventShortDto1, eventShortDto2));

        mvc.perform(get("/events?" + "from=" + eventQueryParams.getFrom()
                + "&size=" + eventQueryParams.getSize() + "&sort=" + eventQueryParams.getSort()
                + "&categories=" + eventQueryParams.getCategories().get(0) + "&text=" + eventQueryParams.getText()
                + "&onlyAvailable=" + eventQueryParams.getOnlyAvailable() + "&paid=" + eventQueryParams.getPaid())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id", is(eventShortDto1.getId()), Long.class))
                .andExpect(jsonPath("$.[1].id", is(eventShortDto2.getId()), Long.class));
        verify(eventService, times(1)).getAll(eventQueryParams, ipAddress, uri);
    }

    @Test
    public void getAll_whenWrongSortQueryParam_thenThrownException() throws Exception {
        mvc.perform(get("/events?" + "from=" + eventQueryParams.getFrom()
                        + "&size=" + eventQueryParams.getSize() + "&sort=" + "hits"
                        + "&categories=" + eventQueryParams.getCategories().get(0)
                        + "&text=" + eventQueryParams.getText()
                        + "&onlyAvailable=" + eventQueryParams.getOnlyAvailable()
                        + "&paid=" + eventQueryParams.getPaid())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(BindException.class, result.getResolvedException()));
        verify(eventService, never()).getAll(eventQueryParams, ipAddress, uri);
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() throws Exception {
        when(eventService.get(eventFullDto.getId(), ipAddress, uri + "/" + eventFullDto.getId()))
                .thenReturn(eventFullDto);

        mvc.perform(get("/events/" + eventFullDto.getId())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Long.class));
        verify(eventService, times(1))
                .get(eventFullDto.getId(), ipAddress, uri + "/" + eventFullDto.getId());
    }

    @Test
    public void get_whenNotValidPathVariable_thenThrownException() throws Exception {
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
