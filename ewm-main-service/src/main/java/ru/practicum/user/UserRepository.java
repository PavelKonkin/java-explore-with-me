package ru.practicum.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
@Query(value = "SELECT u.user_id, u.name, u.email, " +
        " (COALESCE(l.likes_count, 0) - COALESCE(d.dislikes_count, 0)) AS rating " +
        " FROM users u " +
        " LEFT JOIN (SELECT e.initiator_id, COUNT(l.event_id) AS likes_count " +
        "            FROM events e " +
        "            LEFT JOIN event_user_like l ON l.event_id = e.event_id " +
        "            GROUP BY e.initiator_id) l ON l.initiator_id = u.user_id " +
        " LEFT JOIN (SELECT e.initiator_id, COUNT(d.event_id) AS dislikes_count " +
        "            FROM events e " +
        "            LEFT JOIN event_user_dislike d ON d.event_id = e.event_id " +
        "            GROUP BY e.initiator_id) d ON d.initiator_id = u.user_id " +
        " WHERE :idsSize = 0 OR u.user_id IN :ids  " +
        " GROUP BY u.user_id, u.name, u.email, l.likes_count, d.dislikes_count " +
        " ORDER BY u.user_id " +
        " LIMIT COALESCE(:limit, (SELECT COUNT(*) FROM users)) OFFSET COALESCE(:offset, 0)",
        nativeQuery = true)
    List<Object[]> findAllInIds(@Param("ids") List<Long> ids,
                                @Param("idsSize") int idsSize,
                                @Param("limit") Integer limit,
                                @Param("offset") Long offset);

    @Query(value = "SELECT u.user_id, u.name, u.email, " +
            "(COALESCE(l.likes_count, 0) - COALESCE(d.dislikes_count, 0)) AS rating " +
            "FROM users u " +
            "LEFT JOIN (" +
            "   SELECT e.initiator_id, COUNT(l.event_id) AS likes_count " +
            "   FROM events e " +
            "   LEFT JOIN event_user_like l ON l.event_id = e.event_id " +
            "   GROUP BY e.initiator_id" +
            ") l ON l.initiator_id = u.user_id " +
            "LEFT JOIN (" +
            "   SELECT e.initiator_id, COUNT(d.event_id) AS dislikes_count " +
            "   FROM events e " +
            "   LEFT JOIN event_user_dislike d ON d.event_id = e.event_id " +
            "   GROUP BY e.initiator_id" +
            ") d ON d.initiator_id = u.user_id " +
            "WHERE u.user_id = :id " +
            "GROUP BY u.user_id, u.name, u.email, l.likes_count, d.dislikes_count",
            nativeQuery = true)
    List<Object[]> findWithRating(@Param("id") Long id);
}
