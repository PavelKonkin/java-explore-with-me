package ru.practicum.event;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.participationrequest.ParticipationRequest;
import ru.practicum.participationrequest.ParticipationRequestStatus;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {
    public static Specification<Event> containsTextInAnnotationOrDescription(String text) {
        return (root, query, builder) -> {
            if (text == null || text.isEmpty()) {
                return builder.conjunction();
            }
            String likePattern = "%" + text.toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("annotation")), likePattern),
                    builder.like(builder.lower(root.get("description")), likePattern)
            );
        };
    }

    public static Specification<Event> hasEventDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, builder) -> {
            if (startDate == null && endDate == null) {
                return builder.greaterThan(root.get("eventDate"), LocalDateTime.now());
            } else if (startDate == null) {
                return builder.lessThanOrEqualTo(root.get("eventDate"), endDate);
            } else if (endDate == null) {
                return builder.greaterThanOrEqualTo(root.get("eventDate"), startDate);
            } else {
                return builder.between(root.get("eventDate"), startDate, endDate);
            }
        };
    }

    public static Specification<Event> hasPublishedState() {
        return (root, query, builder) -> builder.equal(root.get("state"), EventState.PUBLISHED);
    }

    public static Specification<Event> hasCategoryIdInList(List<Long> categoryIds) {
        return (root, query, builder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return builder.conjunction();
            }
            return root.get("category").get("id").in(categoryIds);
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, builder) -> {
            if (paid == null || paid.equals(false)) {
                return builder.conjunction();
            }
            return builder.equal(root.get("paid"), paid);
        };
    }

    public static Specification<Event> hasConfirmedRequestsLessThanLimit(Boolean onlyAvailable) {
        return (root, query, builder) -> {
            if (!onlyAvailable) {
                return builder.conjunction();
            }

            Join<Event, ParticipationRequest> requests = root.join("participationRequests", JoinType.LEFT);
            query.groupBy(root.get("id"));

            return builder.or(builder.equal(root.get("participantLimit"), 0), builder.and(
                    builder.equal(requests.get("status"), ParticipationRequestStatus.CONFIRMED),
                    builder.lessThan(builder.countDistinct(requests.get("id")), root.get("participantLimit")))
            );

        };
    }

    public static Specification<Event> sortByDate() {
        return (root, query, builder) -> {
            Path<LocalDateTime> eventDate = root.get("eventDate");
            Order orderByDate = builder.asc(eventDate);

            query.orderBy(orderByDate);
            return builder.conjunction();
        };
    }

    public static Specification<Event> hasStateInList(List<EventState> states) {
        return (root, query, builder) -> {
            if (states == null || states.isEmpty()) {
                return builder.conjunction();
            }
            return root.get("state").in(states);
        };
    }

    public static Specification<Event> hasInitiatorIdInList(List<Long> initiatorIds) {
        return (root, query, builder) -> {
            if (initiatorIds == null || initiatorIds.isEmpty()) {
                return builder.conjunction();
            }
            return root.get("initiator").get("id").in(initiatorIds);
        };
    }

    public static Specification<Event> hasInitiatorThatIsNotUser(long userId) {
        return (root, query, builder) -> builder.notEqual(root.get("initiator").get("id"), userId);
    }

    public static Specification<Event> hasConfirmedRequest() {
        return (root, query, builder) -> {

            Join<Event, ParticipationRequest> requests = root.join("participationRequest", JoinType.INNER);
            query.groupBy(root.get("id"));
            query.having(builder.greaterThan(builder.count(requests.get("id")), 0L));

            return builder.equal(requests.get("status"), ParticipationRequestStatus.CONFIRMED);
        };
    }

    public static Specification<Event> hasEventId(long eventId) {
        return (root, query, builder) -> builder.equal(root.get("id"), eventId);
    }
}
