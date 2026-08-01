package ru.practicum.repository;

import ru.practicum.StatsDto;
import ru.practicum.model.EndpointHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("""
            SELECT new ru.practicum.StatsDto(hit.app, hit.uri, COUNT(hit.ip))
            FROM EndpointHit AS hit
            WHERE hit.timestamp BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(hit.ip) DESC
            """)
    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.StatsDto(hit.app, hit.uri, COUNT(DISTINCT hit.ip))
            FROM EndpointHit AS hit
            WHERE hit.timestamp BETWEEN :start AND :end
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(DISTINCT hit.ip) DESC
            """)
    List<StatsDto> getStatsUnique(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.StatsDto(hit.app, hit.uri, COUNT(hit.ip))
            FROM EndpointHit AS hit
            WHERE hit.timestamp BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(hit.ip) DESC
            """)
    List<StatsDto> getStatsByUri(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            SELECT new ru.practicum.StatsDto(hit.app, hit.uri, COUNT(DISTINCT hit.ip))
            FROM EndpointHit AS hit
            WHERE hit.timestamp BETWEEN :start AND :end AND hit.uri IN :uris
            GROUP BY hit.app, hit.uri
            ORDER BY COUNT(DISTINCT hit.ip) DESC
            """)
    List<StatsDto> getStatsByUriUnique(LocalDateTime start, LocalDateTime end, List<String> uris);
}