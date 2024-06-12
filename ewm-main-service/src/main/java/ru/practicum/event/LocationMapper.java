package ru.practicum.event;

import org.springframework.stereotype.Component;
import ru.practicum.event.dto.LocationDto;

@Component
public class LocationMapper {
    public LocationDto convertLocation(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
