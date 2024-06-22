package ru.practicum.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.event.Event;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserRatingService {
    private final UserRepository userRepository;

    @Autowired
    public UserRatingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<Long, Long> getUsersRating(List<Event> events) {
        return getUsersRating(events,null);
    }

    public Map<Long, Long> getUsersRating(List<Event> events, Pageable page) {
        Integer limit = (page == null) ? null : page.getPageSize();
        Long offset = (page == null) ? null : page.getOffset();
        List<Long> userIds = events.stream()
                .map(event -> event.getInitiator().getId())
                .distinct()
                .collect(Collectors.toList());

        List<Object[]> userObj = userRepository.findAllInIds(userIds, userIds.size(), limit, offset);
        return userObj.stream()
                .collect(Collectors.toMap(
                        result -> ((BigInteger) result[0]).longValue(),  // userId
                        result -> ((BigInteger) result[3]).longValue()    // rating
                ));
    }

    public Map<Long, Long> getUserRating(Event events) {
        Long userId = events.getInitiator().getId();
        List<Object[]> userObj = userRepository.findWithRating(userId);
        Map<Long, Long> userRating = new HashMap<>();
        userRating.put(userId, ((BigInteger) userObj.get(0)[3]).longValue());
        return userRating;
    }
}
