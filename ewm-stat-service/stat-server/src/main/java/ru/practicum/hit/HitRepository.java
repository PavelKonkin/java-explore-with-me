package ru.practicum.hit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ViewStatDto;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {
    @Query(" select new ru.practicum.ViewStatDto(count(distinct h.ip), h.app, h.uri)" +
            " from Hit h" +
            " where h.timestamp between ?1 and ?2" +
            " and (coalesce(?3, null) is null or h.uri in ?3)" +
            " group by h.app, h.uri" +
            " order by count(distinct h.ip) desc")
    List<ViewStatDto> findAllUniqueHit(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("select new ru.practicum.ViewStatDto(count(h.ip), h.app, h.uri)" +
            " from Hit h" +
            " where h.timestamp between ?1 and ?2" +
            " and (coalesce(?3, null) is null or h.uri in ?3)" +
            " group by h.app, h.uri" +
            " order by count(h.ip) desc")
    List<ViewStatDto> findAllHit(LocalDateTime start, LocalDateTime end, List<String> uris);
}
