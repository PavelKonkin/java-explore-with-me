package ru.practicum.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RatingService {
    private final EventUserRatingRepository eventUserRatingRepository;

    @Autowired
    public RatingService(EventUserRatingRepository eventUserRatingRepository) {
        this.eventUserRatingRepository = eventUserRatingRepository;
    }

    public Map<Long, Long> getUsersRating(List<Long> userIds) {
        List<Object[]> userObj = eventUserRatingRepository.findUsersRatingByUserIds(userIds);
        return userObj.stream()
                .collect(Collectors.toMap(
                        result -> ((BigInteger) result[0]).longValue(),  // userId
                        result -> ((BigInteger) result[1]).longValue()    // rating
                ));
    }

    public Long getUserRating(Long userId) {
        return eventUserRatingRepository.findUserRatingByUserId(userId);
    }

    public Map<Long, Long> getEventsRating(List<Long> eventIds) {
        List<Object[]> eventObj = eventUserRatingRepository.findRatingOfEventsByEventIds(eventIds, eventIds.size());

        return eventObj.stream()
                .collect(Collectors.toMap(
                        result -> ((BigInteger) result[0]).longValue(),  // eventId
                        result -> ((BigInteger) result[1]).longValue()    // rating
                ));
    }

    public Long getEventRating(Long eventId) {
        return eventUserRatingRepository.findEventRatingByEventId(eventId);
    }
}
