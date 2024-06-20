package ru.practicum.adminapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.StateAdminAction;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.eventutil.EventUtilService;
import ru.practicum.exception.ConflictException;

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
public class EventServiceTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private EventUtilService eventUtilService;
    @InjectMocks
    private EventServiceImpl eventService;

    private AdminEventParams eventQueryParams;
    private Event event1;
    private Event event1Updated;
    private Event event2;
    private EventFullDto eventFullDto1;
    private EventFullDto eventFullDto2;
    private EventFullDto eventFullDto1Updated;
    private EventFullDto eventFullDto1ToUpdate;
    private UpdateEventAdminRequest adminRequestToPublish;
    private UpdateEventAdminRequest adminRequestToCancel;
    private final Map<Long, Integer> hitsByEvent = new HashMap<>();
    private final Map<Long, Long> confirmedRequestCount = new HashMap<>();

    @BeforeEach
    public void setup() {
        Location locationSaved = Location.builder()
                .id(1L)
                .lat(22.22f)
                .lon(33.33f)
                .build();
        LocationDto locationDto = LocationDto.builder()
                .lat(locationSaved.getLat())
                .lon(locationSaved.getLon())
                .build();
        Category category = Category.builder()
                .id(1L)
                .name("category")
                .build();
        CategoryDto categoryDto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
        event1 = Event.builder()
                .id(1L)
                .title("test")
                .state(EventState.PENDING)
                .build();
        event2 = Event.builder()
                .id(2L)
                .state(EventState.PUBLISHED)
                .build();
        eventFullDto1 = EventFullDto.builder()
                .id(event1.getId())
                .build();
        eventFullDto2 = EventFullDto.builder()
                .id(event2.getId())
                .build();
        eventFullDto1Updated = EventFullDto.builder()
                .id(event1.getId())
                .build();
        eventQueryParams = AdminEventParams.builder()
                .from(0)
                .size(10)
                .build();
        hitsByEvent.put(1L, 1);
        hitsByEvent.put(2L, 2);
        confirmedRequestCount.put(1L, 1L);
        adminRequestToPublish = UpdateEventAdminRequest.builder()
                .title("updated")
                .category(1L)
                .location(locationDto)
                .stateAction(StateAdminAction.PUBLISH_EVENT)
                .build();
        adminRequestToCancel = UpdateEventAdminRequest.builder()
                .title("updated")
                .stateAction(StateAdminAction.REJECT_EVENT)
                .build();
        eventFullDto1ToUpdate = EventFullDto.builder()
                .id(event1.getId())
                .title(adminRequestToPublish.getTitle())
                .state(EventState.CANCELED)
                .location(locationDto)
                .category(categoryDto)
                .build();
        eventFullDto1Updated = EventFullDto.builder()
                .id(event1.getId())
                .title(adminRequestToCancel.getTitle())
                .state(EventState.CANCELED)
                .views(1)
                .confirmedRequests(1)
                .location(locationDto)
                .category(categoryDto)
                .build();
        event1Updated = Event.builder()
                .id(1L)
                .title(adminRequestToCancel.getTitle())
                .state(EventState.CANCELED)
                .build();
    }

    @Test
    public void getAll_whenSuccessful_thenReturnListOfEventFullDtos() {
        List<Long> eventIds = List.of(event1.getId(), event2.getId());
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event1, event2)));
        when(eventUtilService.getHitsByEvent(eventIds)).thenReturn(hitsByEvent);
        when(eventUtilService.getConfirmedRequestCountById(eventIds)).thenReturn(confirmedRequestCount);
        when(eventMapper.convertEventToFullDto(event1, hitsByEvent.getOrDefault(event1.getId(), 0),
                confirmedRequestCount.getOrDefault(event1.getId(), 0L))).thenReturn(eventFullDto1);
        when(eventMapper.convertEventToFullDto(event2, hitsByEvent.getOrDefault(event2.getId(), 0),
                confirmedRequestCount.getOrDefault(event2.getId(), 0L))).thenReturn(eventFullDto2);

        List<EventFullDto> actualListOfEventFullDto = eventService.getAll(eventQueryParams);

        assertThat(List.of(eventFullDto1, eventFullDto2), is(actualListOfEventFullDto));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, times(1)).getHitsByEvent(eventIds);
        verify(eventUtilService, times(1)).getConfirmedRequestCountById(eventIds);
        verify(eventMapper, times(2))
                .convertEventToFullDto(any(Event.class), anyInt(), anyLong());
    }

    @Test
    public void getAll_whenNoneEventFound_thenReturnEmptyList() {
        List<Long> eventIds = List.of();
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        List<EventFullDto> actualListOfEventFullDto = eventService.getAll(eventQueryParams);

        assertThat(List.of(), is(actualListOfEventFullDto));
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
        verify(eventUtilService, never()).getHitsByEvent(eventIds);
        verify(eventUtilService, never()).getConfirmedRequestCountById(eventIds);
        verify(eventMapper, never()).convertEventToFullDto(any(Event.class), anyInt(), anyLong());
    }

    @Test
    public void patch_whenSuccessfulRejectRequest_thenReturnUpdatedEventFullDto() {
        eventFullDto1Updated.setViews(0);
        eventFullDto1Updated.setConfirmedRequests(0L);

        when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));
        when(eventRepository.save(event1Updated)).thenReturn(event1Updated);
        when(eventMapper.convertEventToFullDto(event1Updated,0, 0L))
                .thenReturn(eventFullDto1ToUpdate);

        EventFullDto actualEventFullDto = eventService.patch(event1.getId(), adminRequestToCancel);

        assertThat(eventFullDto1Updated, is(actualEventFullDto));
        assertThat(adminRequestToCancel.getTitle(), is(actualEventFullDto.getTitle()));
        assertThat(actualEventFullDto.getState(), is(EventState.CANCELED));
        verify(eventRepository, times(1)).findById(event1.getId());
        verify(eventMapper, times(1)).convertEventToFullDto(event1Updated,0,0L);
    }

    @Test
    public void patch_whenPublishEventWithWrongState_thenThrownException() {
        when(eventRepository.findById(event2.getId())).thenReturn(Optional.of(event2));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event2.getId(), adminRequestToPublish));
        assertThat(exception.getMessage(),
                is("Cannot publish the event because it's not in the right state: " + event2.getState()));
        verify(eventRepository, times(1)).findById(event2.getId());
        verify(eventMapper, never()).convertEventToFullDto(any(Event.class), anyInt(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(List.of(event2.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event2.getId()));
    }

    @Test
    public void patch_whenCancelEventWithWrongState_thenThrownException() {
        when(eventRepository.findById(event2.getId())).thenReturn(Optional.of(event2));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.patch(event2.getId(), adminRequestToCancel));
        assertThat(exception.getMessage(),
                is("Cannot cancel the event because it's not in the right state: " + event2.getState()));
        verify(eventRepository, times(1)).findById(event2.getId());
        verify(eventMapper, never()).convertEventToFullDto(any(Event.class), anyInt(), anyLong());
        verify(eventUtilService, never()).getHitsByEvent(List.of(event2.getId()));
        verify(eventUtilService, never())
                .getConfirmedRequestCountById(List.of(event2.getId()));
    }
}
