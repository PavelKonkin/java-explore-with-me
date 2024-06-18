package ru.practicum.eventutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ViewStatDto;
import ru.practicum.client.StatClient;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventUtilServiceImpl implements EventUtilService {
    private final StatClient statClient;
    private final ParticipationRequestRepository requestRepository;

    @Autowired
    public EventUtilServiceImpl(StatClient statClient, ParticipationRequestRepository requestRepository) {
        this.statClient = statClient;
        this.requestRepository = requestRepository;
    }

    private static final String EVENTS_START = "1970-01-01 00:00:00";
    private static final String EVENTS_END = "2999-12-31 00:00:00";
    private static final String EVENTS_URI = "/events/";

    @Override
    public Map<Long, Integer> getHitsByEvent(List<Long> eventIds) {
        Map<Long, Integer> hitsByEvent = new HashMap<>();

        if (eventIds == null || eventIds.isEmpty()) {
            return hitsByEvent;
        }

        List<String> uris = eventIds.stream()
                .map(el -> EVENTS_URI + el)
                .collect(Collectors.toList());

        List<ViewStatDto> response = statClient.getStat(EVENTS_START, EVENTS_END, uris, true);


        if (response.isEmpty()) {
            return hitsByEvent;
        } else {
            hitsByEvent = response.stream()
                    .collect(Collectors.groupingBy(
                            viewStat -> Long.parseLong(viewStat.getUri().split("/events/")[1]),
                            Collectors.summingInt(viewStat -> viewStat.getHits().intValue())));

        }
        return hitsByEvent;
    }

    @Override
    public Map<Long, Long> getConfirmedRequestCountById(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ParticipationRequest> confirmedRequests = requestRepository
                .findAllByEventIdInAndStatus(eventIds, ParticipationRequestStatus.CONFIRMED);
        return confirmedRequests.stream()
                .collect(Collectors.groupingBy(entry -> entry.getEvent().getId(), Collectors.counting()));
    }
}
