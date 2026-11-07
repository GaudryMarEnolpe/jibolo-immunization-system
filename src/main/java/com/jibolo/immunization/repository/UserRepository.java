package com.jibolo.immunization.repository;
import com.jibolo.immunization.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*; import java.time.*;
public interface UserRepository extends JpaRepository<User, Long> {
Optional<User> findByUsername(String u); List<User> findByRole(User.Role r); boolean existsByUsername(String u);
}
