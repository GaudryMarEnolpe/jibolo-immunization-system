package com.jibolo.immunization.repository;
import com.jibolo.immunization.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*; import java.time.*;
public interface ScheduleRepository extends JpaRepository<ImmunizationSchedule, Long> {
List<ImmunizationSchedule> findAllByOrderByScheduledDateAsc();
  List<ImmunizationSchedule> findByChildParentOrderByScheduledDateAsc(User p);
  List<ImmunizationSchedule> findByStatusInAndScheduledDateBetween(Collection<ImmunizationSchedule.Status> s, LocalDate a, LocalDate b);
}
