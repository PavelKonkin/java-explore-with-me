package ru.practicum.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(" select new ru.practicum.user.dto.UserDto(u.id, u.name, u.email, cast(0 as java.lang.Long) )" +
            " from User u" +
            " where " +
            " (:listSize = 0  or u.id in :ids)")
    List<UserDto> findAllInIds(@Param("ids") List<Long> ids, @Param("listSize") int listSize, Pageable page);
}
