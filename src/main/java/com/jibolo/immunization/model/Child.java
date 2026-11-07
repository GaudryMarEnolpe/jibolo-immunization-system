package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
import java.time.*; import java.time.temporal.ChronoUnit; import java.util.*;
@Entity @Table(name = "child_records") @Getter @Setter @NoArgsConstructor
public class Child {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Version @Column(columnDefinition = "bigint not null default 0") private long version;
  private String fullName;
  private LocalDate birthDate;
  private String sex;
  private String motherName;
  private String fatherName;
  private String purok;
  private Double birthWeight;
  private Double height;   // cm
  private Double weight;   // kg
  private Double bmi;      // auto-calculated: kg / (m x m)
  private String placeOfDelivery;
  private boolean consent;
  private LocalDate consentDate;
  @ManyToOne private User parent;
  @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("scheduledDate ASC, id ASC")
  private List<ImmunizationSchedule> schedules = new ArrayList<>();
  @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RescheduleRequest> requests = new ArrayList<>();

  @PrePersist @PreUpdate
  protected void calcBmi() {
    bmi = (height != null && weight != null && height > 0) ? Math.round(weight / Math.pow(height / 100.0, 2) * 10) / 10.0 : null;
  }
  public String getBirthText() { return Fmt.d(birthDate); }
  public String getAgeText() {
    if (birthDate == null) return "-";
    Period p = Period.between(birthDate, LocalDate.now());
    if (p.getYears() > 0) return p.getYears() + " yr " + p.getMonths() + " mo";
    if (p.getMonths() > 0) return p.getMonths() + " mo " + p.getDays() + " d";
    return ChronoUnit.DAYS.between(birthDate, LocalDate.now()) + " days";
  }
  public long getDone() { return schedules.stream().filter(s -> s.getStatus() == ImmunizationSchedule.Status.VACCINATED).count(); }
  public int getPercent() { return schedules.isEmpty() ? 0 : (int) (getDone() * 100 / schedules.size()); }
  public boolean isFullyImmunized() { return !schedules.isEmpty() && getDone() == schedules.size(); }
  public boolean isHasOverdue() { return schedules.stream().anyMatch(ImmunizationSchedule::isOverdue); }
  public ImmunizationSchedule getNext() {
    return schedules.stream().filter(s -> s.getStatus() != ImmunizationSchedule.Status.VACCINATED).min(Comparator.comparing(ImmunizationSchedule::getScheduledDate)).orElse(null);
  }
  public String getStatusText() { return isFullyImmunized() ? "Fully immunized" : isHasOverdue() ? "Overdue" : "On track"; }
}
