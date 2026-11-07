package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
import java.time.*;
/** A parent request: either to reschedule an existing dose (schedule != null) or to request a vaccine (schedule == null). */
@Entity @Getter @Setter @NoArgsConstructor
public class RescheduleRequest {
  public enum Status { PENDING, APPROVED, REJECTED }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Version @Column(columnDefinition = "bigint not null default 0") private long version;
  @ManyToOne private ImmunizationSchedule schedule;
  @ManyToOne private Child child;
  @ManyToOne private Vaccine vaccine;
  @ManyToOne private User parent;
  private LocalDate preferredDate;
  @Column(length = 500) private String reason;
  @Enumerated(EnumType.STRING) private Status status = Status.PENDING;
  @Column(length = 500) private String response;
  private LocalDateTime createdAt = LocalDateTime.now();
  public String getTypeText() { return schedule == null ? "Vaccine request" : "Reschedule request"; }
  public String getPreferredText() { return Fmt.d(preferredDate); }
  public String getTimeText() { return Fmt.dt(createdAt); }
}
