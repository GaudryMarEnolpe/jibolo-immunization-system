package com.jibolo.immunization.repository;
import com.jibolo.immunization.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*; import java.time.*;
public interface VaccineRepository extends JpaRepository<Vaccine, Long> {
Optional<Vaccine> findByCode(String c);
}
