package ru.practicum.event;

import lombok.*;
import ru.practicum.category.Category;
import ru.practicum.user.User;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@NamedEntityGraph(name = "event.category.location.user", attributeNodes = {
        @NamedAttributeNode("category"),
        @NamedAttributeNode("location"),
        @NamedAttributeNode("initiator")
})
@Table(name = "events")
@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @OneToOne
    @JoinColumn(name = "location_id")
    @ToString.Exclude
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    @ToString.Exclude
    private User initiator;

    private String annotation;
    private String description;
    private String title;

    @Enumerated(EnumType.STRING)
    private EventState state;

    private boolean paid;

    @Column(name = "participant_limit")
    private int participantLimit;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "request_moderation")
    private boolean requestModeration;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
