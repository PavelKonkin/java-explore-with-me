package ru.practicum.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.user.User;

import java.util.List;
import java.util.Optional;

public interface EventUserRatingRepository extends JpaRepository<EventUserRating, Long> {
    Optional<EventUserRating> findByEventAndUser(Event event, User user);

    @Query(value = " SELECT eur.event_id AS eventId," +
            " COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0) AS rating " +
            " FROM event_user_rating eur " +
            " WHERE :size = 0 or eur.event_id IN :ids " +
            " GROUP BY eventId", nativeQuery = true)
    List<Object[]> findRatingOfEventsByEventIds(@Param("ids") List<Long> ids, @Param("size") long size);

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

    @Query(value = " SELECT u.user_id," +
            " COALESCE(SUM(CASE eur.liked WHEN TRUE THEN 1 WHEN FALSE THEN -1 ELSE 0 END), 0) AS rating " +
            " FROM users u " +
            " LEFT JOIN events e ON e.initiator_id = u.user_id" +
            " LEFT JOIN event_user_rating eur ON eur.event_id = e.event_id " +
            " WHERE u.user_id IN :userIds " +
            " GROUP BY u.user_id",
            nativeQuery = true)
    List<Object[]> findUsersRatingByUserIds(@Param("userIds") List<Long> userIds);
}
