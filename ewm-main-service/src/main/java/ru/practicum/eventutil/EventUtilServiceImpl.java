package ru.practicum.eventutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestRepository;
import ru.practicum.participationrequest.ParticipationRequestStatus;

import java.util.ArrayList;
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
        List<String> uris = eventIds.stream()
                .map(el -> EVENTS_URI + el)
                .collect(Collectors.toList());

        ArrayList<HashMap<String, Object>> arrayListResponse;
        Map<Long, Integer> hitsByEvent = new HashMap<>();

        ResponseEntity<Object> response;
        try {
            response = statClient.getStat(EVENTS_START, EVENTS_END, uris, true);
        } catch (Throwable e) {
            return hitsByEvent;
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            Object responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();

            if (responseBody instanceof List) {
                try {
                    arrayListResponse = objectMapper.convertValue(responseBody, ArrayList.class);
                    for (HashMap<String, Object> map : arrayListResponse) {
                        String uri = (String) map.get("uri");
                        long key = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                        Integer hit = (Integer) map.get("hits");
                        hitsByEvent.put(key, hitsByEvent.getOrDefault(key, 0) + hit);
                    }
                } catch (ClassCastException ignored) {
                }
            }
        }
        return hitsByEvent;
    }

    @Override
    public Map<Long, Long> getConfirmedRequestCountById(List<Long> eventIds) {
        List<ParticipationRequest> confirmedRequests = requestRepository
                .findAllByEventIdInAndStatus(eventIds, ParticipationRequestStatus.CONFIRMED);
        return confirmedRequests.stream()
                .collect(Collectors.groupingBy(entry -> entry.getEvent().getId(), Collectors.counting()));
    }
}
