package ru.practicum.compilation;

import lombok.*;
import ru.practicum.event.Event;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@NamedEntityGraph(
        name = "compilation.event.initiator.location.category",
        attributeNodes = {
                @NamedAttributeNode(value = "events", subgraph = "events-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "events-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("initiator"),
                                @NamedAttributeNode("location"),
                                @NamedAttributeNode("category")
                        }
                )
        })
@Table(name = "compilations")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compilation_id")
    private Long id;
    private String title;
    private boolean pinned;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_compilation",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    @ToString.Exclude
    private Set<Event> events = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Compilation compilation = (Compilation) o;
        return Objects.equals(id, compilation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
