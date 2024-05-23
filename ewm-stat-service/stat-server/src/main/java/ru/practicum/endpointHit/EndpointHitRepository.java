package ru.practicum.endpointHit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    @Query("select count(distinct eh.ip) as hits, eh.app as app, eh.uri as uri" +
            " from EndpointHit eh" +
            " where eh.timestamp between ?1 and ?2" +
            " and (coalesce(?3, null) is null or eh.uri in ?3)" +
            " group by eh.app, eh.uri" +
            " order by hits desc")
    List<EndpointHitProjection> findAllUniqueHit(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("select count(eh.ip) as hits, eh.app as app, eh.uri as uri" +
            " from EndpointHit eh" +
            " where eh.timestamp between ?1 and ?2" +
            " and (coalesce(?3, null) is null or eh.uri in ?3)" +
            " group by eh.app, eh.uri" +
            " order by hits desc")
    List<EndpointHitProjection> findAllHit(LocalDateTime start, LocalDateTime end, List<String> uris);
}
