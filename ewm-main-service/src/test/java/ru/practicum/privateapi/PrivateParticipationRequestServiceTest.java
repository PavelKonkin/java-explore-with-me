package ru.practicum.privateapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestMapper;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PrivateParticipationRequestServiceTest {
    @Mock
    private ParticipationRequestRepository participationRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ParticipationRequestMapper participationRequestMapper;
    @InjectMocks
    private PrivateParticipationRequestServiceImpl participationRequestService;

    private User user;
    private Event event;
    private Event eventSameRequester;
    private Event eventNotPublished;
    private Event eventWithParticipantLimit;
    private ParticipationRequest participationRequest;
    private ParticipationRequest participationRequestCanceled;
    private ParticipationRequest participationRequestToSave;
    private ParticipationRequestDto participationRequestDto;
    private ParticipationRequestDto participationRequestCanceledDto;

    @BeforeEach
    public void setup() {
        user = User.builder()
                .id(1L)
                .build();
        User user2 = User.builder()
                .id(2L)
                .build();
        event = Event.builder()
                .id(1L)
                .initiator(user2)
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .build();
        eventSameRequester = Event.builder()
                .id(2L)
                .initiator(user)
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .build();
        eventNotPublished = Event.builder()
                .id(3L)
                .initiator(user2)
                .state(EventState.PENDING)
                .participantLimit(0)
                .build();
        eventWithParticipantLimit = Event.builder()
                .id(3L)
                .initiator(user2)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .build();
        participationRequest = ParticipationRequest.builder()
                .id(1L)
                .requester(user)
                .event(event)
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();
        participationRequestCanceled = ParticipationRequest.builder()
                .id(1L)
                .requester(user)
                .event(event)
                .status(ParticipationRequestStatus.REJECTED)
                .build();
        participationRequestToSave = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .status(ParticipationRequestStatus.CONFIRMED)
                .created(participationRequest.getCreated())
                .build();
        participationRequestDto = ParticipationRequestDto.builder()
                .id(participationRequest.getId())
                .requester(user.getId())
                .event(event.getId())
                .created(participationRequest.getCreated())
                .status(ParticipationRequestStatus.CONFIRMED.name())
                .build();
        participationRequestCanceledDto = ParticipationRequestDto.builder()
                .id(participationRequestCanceled.getId())
                .requester(user.getId())
                .event(event.getId())
                .created(participationRequestCanceled.getCreated())
                .status(ParticipationRequestStatus.REJECTED.name())
                .build();
    }

    @Test
    public void add_whenSuccessful_thenReturnParticipationRequestDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(participationRequestRepository.save(participationRequestToSave)).thenReturn(participationRequest);
        when(participationRequestMapper.convertParticipationRequest(participationRequest))
                .thenReturn(participationRequestDto);

        ParticipationRequestDto actualResult = participationRequestService
                .add(user.getId(), event.getId(), participationRequest.getCreated());

        assertThat(participationRequestDto, is(actualResult));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findById(event.getId());
        verify(participationRequestRepository, times(1)).save(participationRequestToSave);
        verify(participationRequestMapper, times(1))
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void add_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.add(user.getId(), event.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("User with id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, never()).findById(event.getId());
        verify(participationRequestRepository, never()).save(participationRequestToSave);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void add_whenEventNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.add(user.getId(), event.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Event with id=" + event.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findById(event.getId());
        verify(participationRequestRepository, never()).save(participationRequestToSave);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void add_whenEventNotPublished_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventNotPublished.getId())).thenReturn(Optional.of(eventNotPublished));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationRequestService.add(user.getId(), eventNotPublished.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Attempt of request participation in not published event"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findById(eventNotPublished.getId());
        verify(participationRequestRepository, never()).save(participationRequestToSave);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void add_whenRequesterIsEventInitiator_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventSameRequester.getId())).thenReturn(Optional.of(eventSameRequester));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationRequestService.add(user.getId(), eventSameRequester.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Initiator of event cannot request participation in it"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findById(eventSameRequester.getId());
        verify(participationRequestRepository, never()).save(participationRequestToSave);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void add_whenLimitOfParticipantExceeded_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventWithParticipantLimit.getId()))
                .thenReturn(Optional.of(eventWithParticipantLimit));
        when(participationRequestRepository
                .findAllByEventIdAndStatus(eventWithParticipantLimit.getId(), ParticipationRequestStatus.CONFIRMED))
                .thenReturn(List.of(participationRequest, participationRequest,
                        participationRequest, participationRequest, participationRequest));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> participationRequestService.add(user.getId(),
                        eventWithParticipantLimit.getId(), LocalDateTime.now()));
        assertThat(exception.getMessage(), is("Limit of participants of event is exceeded"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(eventRepository, times(1)).findById(eventWithParticipantLimit.getId());
        verify(participationRequestRepository, times(1))
                .findAllByEventIdAndStatus(eventWithParticipantLimit.getId(), ParticipationRequestStatus.CONFIRMED);
        verify(participationRequestRepository, never()).save(participationRequestToSave);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void get_whenSuccessful_theReturnListOfParticipationRequestDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(participationRequestRepository.findAllByRequesterId(user.getId()))
                .thenReturn(List.of(participationRequest));
        when(participationRequestMapper.convertParticipationRequest(participationRequest))
                .thenReturn(participationRequestDto);

        List<ParticipationRequestDto> actualResult = participationRequestService.get(user.getId());

        assertThat(List.of(participationRequestDto), is(actualResult));
        verify(userRepository, times(1)).findById(user.getId());
        verify(participationRequestRepository, times(1)).findAllByRequesterId(user.getId());
        verify(participationRequestMapper, times(1))
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void get_whenUserNotFound_theThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.get(user.getId()));

        assertThat(exception.getMessage(), is("User with id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(participationRequestRepository, never()).findAllByRequesterId(user.getId());
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequest);
    }

    @Test
    public void cancel_whenSuccessful_thenReturnParticipationRequestDto() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(participationRequestRepository.findByIdAndRequesterId(user.getId(), participationRequest.getId()))
                .thenReturn(Optional.of(participationRequest));
        when(participationRequestRepository.save(participationRequestCanceled))
                .thenReturn(participationRequestCanceled);
        when(participationRequestMapper.convertParticipationRequest(participationRequestCanceled))
                .thenReturn(participationRequestCanceledDto);

        ParticipationRequestDto actualResult = participationRequestService
                .cancel(user.getId(), participationRequest.getId());

        assertThat(participationRequestCanceledDto, is(actualResult));
        verify(userRepository, times(1)).findById(user.getId());
        verify(participationRequestRepository, times(1))
                .findByIdAndRequesterId(user.getId(), participationRequest.getId());
        verify(participationRequestRepository, times(1)).save(participationRequestCanceled);
        verify(participationRequestMapper, times(1))
                .convertParticipationRequest(participationRequestCanceled);
    }

    @Test
    public void cancel_whenUserNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.cancel(user.getId(), participationRequest.getId()));

        assertThat(exception.getMessage(), is("User with id=" + user.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(participationRequestRepository, never())
                .findById(participationRequest.getId());
        verify(participationRequestRepository, never()).save(participationRequestCanceled);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequestCanceled);
    }

    @Test
    public void cancel_whenRequestNotFound_thenThrownException() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(participationRequestRepository.findByIdAndRequesterId(user.getId(), participationRequest.getId()))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> participationRequestService.cancel(user.getId(), participationRequest.getId()));

        assertThat(exception.getMessage(),
                is("Request with id=" + participationRequest.getId() + " was not found"));
        verify(userRepository, times(1)).findById(user.getId());
        verify(participationRequestRepository, times(1))
                .findByIdAndRequesterId(user.getId(), participationRequest.getId());
        verify(participationRequestRepository, never()).save(participationRequestCanceled);
        verify(participationRequestMapper, never())
                .convertParticipationRequest(participationRequestCanceled);
    }
}
