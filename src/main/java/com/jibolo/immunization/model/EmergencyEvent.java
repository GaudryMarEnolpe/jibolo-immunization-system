package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor
public class EmergencyEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String reason;
  @Column(length = 500) private String message;
  private LocalDate fromDate;
  private LocalDate toDate;
  private LocalDate newDate;
  private String createdBy;
  private int affected;
  private LocalDateTime createdAt = LocalDateTime.now();
  public String getRangeText() { return fromDate.equals(toDate) ? Fmt.d(fromDate) : Fmt.d(fromDate) + " to " + Fmt.d(toDate); }
  public String getNewText() { return Fmt.d(newDate); }
  public String getTimeText() { return Fmt.dt(createdAt); }
}
