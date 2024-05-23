package ru.practicum.endpointHit;

import org.springframework.stereotype.Service;
import ru.practicum.requestHit.dto.EndpointHitDto;
import ru.practicum.requestHit.dto.ViewStatDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EndpointHitServiceImpl implements EndpointHitService {
    private final EndpointHitMapper endpointHitMapper;
    private final EndpointHitRepository endpointHitRepository;

    public EndpointHitServiceImpl(EndpointHitMapper endpointHitMapper, EndpointHitRepository endpointHitRepository) {
        this.endpointHitMapper = endpointHitMapper;
        this.endpointHitRepository = endpointHitRepository;
    }

    @Override
    public void hits(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = endpointHitMapper.toModel(endpointHitDto);
        endpointHitRepository.save(endpointHit);
    }

    @Override
    public List<ViewStatDto> getStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        List<EndpointHitProjection> hits;
        if (unique) {
            hits = endpointHitRepository.findAllUniqueHit(start, end, uris);
        } else {
            hits = endpointHitRepository.findAllHit(start, end, uris);
        }

        return convertToViewStatDtoList(hits);
    }

    private List<ViewStatDto> convertToViewStatDtoList(List<EndpointHitProjection> hits) {
        List<ViewStatDto> result = new ArrayList<>();
        for (EndpointHitProjection hit : hits) {
            ViewStatDto viewStatDto = ViewStatDto.builder()
                    .app(hit.getApp())
                    .uri(hit.getUri())
                    .hits(hit.getHits())
                    .build();
            result.add(viewStatDto);
        }
        return result;
    }
}
