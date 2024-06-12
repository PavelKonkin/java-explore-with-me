package ru.practicum.publicapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.client.StatClient;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicEventServiceTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private StatClient statClient;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventUtilService eventUtilService;
    @InjectMocks
    private PublicEventServiceImpl eventService;

    private EventQueryParams eventQueryParams;
    private Event event1;
    private Event event2;
    private Event event3NotPublished;
    private EventShortDto eventShortDto1;
    private EventShortDto eventShortDto2;
    private EventFullDto eventFullDto;
    private final Map<Long, Integer> hitsByEvent = new HashMap<>();
    private final Map<Long, Long> confirmedRequestCount = new HashMap<>();

    private final String uri = "/events";
    private final String ipAddress = "111.0.0.0";

    @BeforeEach
    public void setup() {
        event1 = Event.builder()
                .id(1L)
                .state(EventState.PUBLISHED)
                .build();
        event2 = Event.builder()
                .id(2L)
                .build();
        event3NotPublished = Event.builder()
                .id(3L)
                .state(EventState.PENDING)
                .build();
        eventShortDto1 = EventShortDto.builder()
                .id(event1.getId())
                .build();
        eventShortDto2 = EventShortDto.builder()
                .id(event2.getId())
                .build();
        eventFullDto = EventFullDto.builder()
                .id(event1.getId())
                .build();
        eventQueryParams = EventQueryParams.builder()
                .from(0)
                .size(10)
                .sort("EVENT_DATE")
                .text("test")
                .onlyAvailable(false)
                .categories(List.of(1L))
                .paid(false)
                .build();
        hitsByEvent.put(1L, 1);
        hitsByEvent.put(2L, 2);
        confirmedRequestCount.put(1L, 1L);
    }

    @Test
    public void getAll_whenSortByViews_thenReturnListOfEventShortDto() {
        List<Long> eventIds = List.of(event1.getId(), event2.getId());
        eventQueryParams.setSort("VIEWS");

        doNothing().when(statClient)
               .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
       when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event1, event2));
       when(eventUtilService.getHitsByEvent(eventIds)).thenReturn(hitsByEvent);
       when(eventUtilService.getConfirmedRequestCountById(eventIds)).thenReturn(confirmedRequestCount);
       when(eventMapper.convertEventToShortDto(event1)).thenReturn(eventShortDto1);
       when(eventMapper.convertEventToShortDto(event2)).thenReturn(eventShortDto2);

       List<EventShortDto> actualEventShortDtos = eventService.getAll(eventQueryParams, uri, ipAddress);

       assertThat(List.of(eventShortDto2, eventShortDto1), is(actualEventShortDtos));
       verify(statClient, times(1))
               .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
       verify(eventRepository, times(1)).findAll(any(Specification.class));
       verify(eventUtilService, times(1))
               .getHitsByEvent(eventIds);
       verify(eventUtilService, times(1))
               .getConfirmedRequestCountById(eventIds);
       verify(eventMapper, times(2)).convertEventToShortDto(any(Event.class));
    }

    @Test
    public void getAll_whenSortByDate_thenReturnListOfEventShortDto() {
        List<Long> eventIds = List.of(event1.getId(), event2.getId());

        doNothing().when(statClient)
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event1, event2)));
        when(eventUtilService.getHitsByEvent(eventIds))
                .thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds))
                .thenReturn(confirmedRequestCount);
        when(eventMapper.convertEventToShortDto(event1)).thenReturn(eventShortDto1);
        when(eventMapper.convertEventToShortDto(event2)).thenReturn(eventShortDto2);

        List<EventShortDto> actualEventShortDtos = eventService.getAll(eventQueryParams, uri, ipAddress);

        assertThat(List.of(eventShortDto1, eventShortDto2), is(actualEventShortDtos));
        verify(statClient, times(1))
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, times(1))
                .getHitsByEvent(eventIds);
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2)).convertEventToShortDto(any(Event.class));
    }

    @Test
    public void get_whenSuccessful_thenReturnEventFullDto() {
        when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));
        when(eventMapper.convertEventToFullDto(event1)).thenReturn(eventFullDto);
        doNothing().when(statClient)
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        when(eventUtilService.getHitsByEvent(List.of(event1.getId())))
                .thenReturn(hitsByEvent);
        when(eventUtilService
                .getConfirmedRequestCountById(List.of(event1.getId())))
                .thenReturn(confirmedRequestCount);

        EventFullDto actualEventFullDto = eventService.get(event1.getId(), uri, ipAddress);

        assertThat(eventFullDto, is(actualEventFullDto));
        verify(eventRepository, times(1)).findById(event1.getId());
        verify(eventMapper, times(1)).convertEventToFullDto(event1);
        verify(statClient, times(1))
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, times(1))
                .getHitsByEvent(List.of(event1.getId()));
        verify(eventUtilService, times(1))
                .getConfirmedRequestCountById(List.of(event1.getId()));
    }

    @Test
    public void get_whenEventNotFound_thenThrownException() {
        long wrongId = 66L;
        when(eventRepository.findById(wrongId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.get(wrongId, uri, ipAddress));
        assertThat(exception.getMessage(), is("Event with id=" + wrongId + " was not found"));
        verify(eventRepository, times(1)).findById(wrongId);
        verify(eventMapper, never()).convertEventToFullDto(any(Event.class));
        verify(statClient, never())
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(wrongId));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(wrongId));
    }

    @Test
    public void get_whenEventNotPublished_thenThrownException() {
        when(eventRepository.findById(event3NotPublished.getId())).thenReturn(Optional.of(event3NotPublished));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.get(event3NotPublished.getId(), uri, ipAddress));
        assertThat(exception.getMessage(), is("Event must be published"));
        verify(eventRepository, times(1)).findById(event3NotPublished.getId());
        verify(eventMapper, never()).convertEventToFullDto(any(Event.class));
        verify(statClient, never())
                .hits(any(String.class), any(String.class), any(String.class), any(LocalDateTime.class));
        verify(eventUtilService, never())
                .getHitsByEvent(List.of(event3NotPublished.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event3NotPublished.getId()));
    }
}
