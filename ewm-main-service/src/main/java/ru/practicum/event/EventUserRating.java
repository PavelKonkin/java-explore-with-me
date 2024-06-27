package ru.practicum.event;


import lombok.*;
import ru.practicum.user.User;

import javax.persistence.*;

@Entity
@Table(name = "event_user_rating",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
public class EventUserRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_user_rating_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "liked", nullable = false)
    private Boolean liked;
}
