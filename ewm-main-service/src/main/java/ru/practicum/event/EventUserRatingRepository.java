package ru.practicum.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.dto.RatingDto;
import ru.practicum.user.User;

import java.util.List;
import java.util.Optional;

public interface EventUserRatingRepository extends JpaRepository<EventUserRating, Long> {
    Optional<EventUserRating> findByEventAndUser(Event event, User user);

    @Query(value = " SELECT new ru.practicum.event.dto.RatingDto(eur.event.id," +
            " COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0)) " +
            " FROM EventUserRating eur " +
            " WHERE :ids IS NULL or eur.event.id IN :ids " +
            " GROUP BY eur.event.id")
    List<RatingDto> findRatingOfEventsByEventIds(@Param("ids") List<Long> ids);

    @Query(value = " SELECT COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0) AS rating " +
            " FROM event_user_rating eur " +
            " WHERE eur.event_id = :eventId ", nativeQuery = true)
    Long findEventRatingByEventId(long eventId);

    @Query(value = " SELECT COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0) AS rating " +
            " FROM users u " +
            " LEFT JOIN events e ON e.initiator_id = u.user_id" +
            " LEFT JOIN event_user_rating eur ON eur.event_id = e.event_id " +
            " WHERE u.user_id = :userId ",
            nativeQuery = true)
    Long findUserRatingByUserId(@Param("userId") Long userId);

    @Query(value = " SELECT new ru.practicum.event.dto.RatingDto(u.id," +
            " COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0))" +
            " FROM User u " +
            " LEFT JOIN Event e ON e.initiator.id = u.id" +
            " LEFT JOIN EventUserRating eur ON eur.event.id = e.id " +
            " WHERE u.id IN :userIds " +
            " GROUP BY u.id")
    List<RatingDto> findUsersRatingByUserIds(@Param("userIds") List<Long> userIds);
}
