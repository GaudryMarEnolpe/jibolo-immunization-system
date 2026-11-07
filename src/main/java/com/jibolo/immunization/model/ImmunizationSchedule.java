package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
import java.time.LocalDate; import java.util.*;
@Entity @Getter @Setter @NoArgsConstructor
public class ImmunizationSchedule {
  public enum Status { SCHEDULED, RESCHEDULE_REQUESTED, VACCINATED, ABSENT }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Version @Column(columnDefinition = "bigint not null default 0") private long version;
  @ManyToOne(optional = false) private Child child;
  @ManyToOne(optional = false) private Vaccine vaccine;
  private int doseNumber = 1;
  private LocalDate scheduledDate;
  @Enumerated(EnumType.STRING) private Status status = Status.SCHEDULED;
  private LocalDate dateGiven;
  private Double temperature;
  private Double weight;
  private Double height;
  @Column(length = 500) private String remarks;
  private String administeredBy;
  private int absentCount;
  @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RescheduleRequest> requests = new ArrayList<>();

  public boolean isOverdue() { return (status == Status.SCHEDULED || status == Status.ABSENT) && scheduledDate != null && scheduledDate.isBefore(LocalDate.now()); }
  public String getLabel() { return vaccine.getTotalDoses() > 1 ? vaccine.getName() + " - Dose " + doseNumber : vaccine.getName(); }
  public String getDueText() {
    if (status == Status.VACCINATED || scheduledDate == null) return "";
    long d = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate);
    return d < 0 ? "Overdue by " + (-d) + (d == -1 ? " day" : " days") : d == 0 ? "Due today" : d == 1 ? "Due tomorrow" : "Due in " + d + " days";
  }
  public String getDueLevel() {
    long d = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate == null ? LocalDate.now() : scheduledDate);
    return status == Status.VACCINATED ? "success" : d < 0 ? "danger" : d <= 7 ? "warning" : "success";
  }
  public String getDateText() { return Fmt.d(scheduledDate); }
  public String getGivenText() { return Fmt.d(dateGiven); }
}
