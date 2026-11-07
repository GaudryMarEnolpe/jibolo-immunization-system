package com.jibolo.immunization.repository;
import com.jibolo.immunization.model.*;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface ChildRepository extends JpaRepository<Child, Long> {
  List<Child> findByParentOrderByFullName(User p);
  /** pass the pattern already wrapped, e.g. "%maria%" */
  @Query("select c from Child c where lower(c.fullName) like :q or lower(c.motherName) like :q or lower(coalesce(c.purok,'')) like :q order by c.fullName")
  List<Child> search(@Param("q") String q);
}
