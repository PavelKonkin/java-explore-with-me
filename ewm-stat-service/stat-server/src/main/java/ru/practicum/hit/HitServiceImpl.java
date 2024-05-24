package ru.practicum.hit;

import org.springframework.stereotype.Service;
import ru.practicum.HitDto;
import ru.practicum.ViewStatDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HitServiceImpl implements HitService {
    private final HitMapper hitMapper;
    private final HitRepository hitRepository;

    public HitServiceImpl(HitMapper hitMapper, HitRepository hitRepository) {
        this.hitMapper = hitMapper;
        this.hitRepository = hitRepository;
    }

    @Override
    public void hits(HitDto hitDto) {
        Hit hit = hitMapper.toModel(hitDto);
        hitRepository.save(hit);
    }

    @Override
    public List<ViewStatDto> getStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        List<ViewStatDto> hits;
        if (uris == null || uris.isEmpty()) {
            uris = List.of();
        }
        if (unique) {
            hits = hitRepository.findAllUniqueHit(start, end, uris);
        } else {
            hits = hitRepository.findAllHit(start, end, uris);
        }

        return hits;
    }
}
