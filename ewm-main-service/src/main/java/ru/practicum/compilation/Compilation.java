package ru.practicum.compilation;

import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Entity
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
