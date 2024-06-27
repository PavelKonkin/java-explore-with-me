package ru.practicum.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.category.CategoryMapper;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.user.UserMapper;

@Component
public class EventMapper {
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;

    @Autowired
    public EventMapper(CategoryMapper categoryMapper, UserMapper userMapper, LocationMapper locationMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.locationMapper = locationMapper;
    }

    public EventShortDto convertEventToShortDto(Event event,
                                                Integer views,
                                                Long confirmedRequests,
                                                Long rating,
                                                Long userRating) {
        return EventShortDto.builder()
                .paid(event.isPaid())
                .title(event.getTitle())
                .eventDate(event.getEventDate())
                .id(event.getId())
                .category(categoryMapper.convertCategory(event.getCategory()))
                .annotation(event.getAnnotation())
                .initiator(userMapper.convertUserToShortDto(event.getInitiator(), userRating))
                .views(views)
                .confirmedRequests(confirmedRequests)
                .rating(rating)
                .build();
    }

    public EventFullDto convertEventToFullDto(Event event,
                                              Integer views,
                                              Long confirmedRequests,
                                              Long rating,
                                              Long userRating) {
        return EventFullDto.builder()
                .id(event.getId())
                .eventDate(event.getEventDate())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .description(event.getDescription())
                .initiator(userMapper.convertUserToShortDto(event.getInitiator(),
                                userRating))
                .category(categoryMapper.convertCategory(event.getCategory()))
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .paid(event.isPaid())
                .location(locationMapper.convertLocation(event.getLocation()))
                .participantLimit(event.getParticipantLimit())
                .state(event.getState())
                .requestModeration(event.isRequestModeration())
                .views(views)
                .confirmedRequests(confirmedRequests)
                .rating(rating)
                .build();
    }

}
