package com.jibolo.immunization.repository;
import com.jibolo.immunization.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*; import java.time.*;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
List<Notification> findByUserOrderByCreatedAtDesc(User u); long countByUserAndSeenFalse(User u);
}
