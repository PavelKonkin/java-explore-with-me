package ru.practicum.endpointHit;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "endpoint_hits")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endpoint_hit_id")
    private Long id;
    private String app;
    private String uri;
    private String ip;
    private LocalDateTime timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointHit endpointHit = (EndpointHit) o;
        return Objects.equals(id, endpointHit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
