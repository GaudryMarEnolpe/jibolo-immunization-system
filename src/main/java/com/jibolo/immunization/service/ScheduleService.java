package com.jibolo.immunization.service;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.model.ImmunizationSchedule.Status; import com.jibolo.immunization.repository.*;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.util.*;

/** All scheduling rules: DOH/EPI schedule, vaccination, absence, reschedule, mass reschedule. */
@Service @RequiredArgsConstructor @Transactional
public class ScheduleService {
  private record Plan(String code, int dose, int days) {}
  // Philippine EPI: BCG + HepB at birth; Penta/OPV/PCV at 6,10,14 wks; IPV 14 wks & 9 mos; MR/MMR 9 & 12 mos
  private static final List<Plan> PLAN = List.of(
    new Plan("BCG", 1, 0), new Plan("HEPB", 1, 0),
    new Plan("PENTA", 1, 42), new Plan("OPV", 1, 42), new Plan("PCV", 1, 42),
    new Plan("PENTA", 2, 70), new Plan("OPV", 2, 70), new Plan("PCV", 2, 70),
    new Plan("PENTA", 3, 98), new Plan("OPV", 3, 98), new Plan("PCV", 3, 98), new Plan("IPV", 1, 98),
    new Plan("IPV", 2, 270), new Plan("MMR", 1, 270), new Plan("MMR", 2, 365));

  private final ScheduleRepository schedules; private final ChildRepository children; private final VaccineRepository vaccines; private final RequestRepository requests;
  private final EmergencyRepository emergencies; private final NotificationService notifier;

  private ImmunizationSchedule get(Long id) { return schedules.findById(id).orElseThrow(); }
  private User parentOf(ImmunizationSchedule s) { return s.getChild().getParent(); }

  public void generate(Child c) {
    for (Plan p : PLAN) {
      ImmunizationSchedule s = new ImmunizationSchedule();
      s.setChild(c); s.setVaccine(vaccines.findByCode(p.code()).orElseThrow()); s.setDoseNumber(p.dose());
      s.setScheduledDate(c.getBirthDate().plusDays(p.days())); c.getSchedules().add(s);
    }
  }

  public void vaccinate(Long id, LocalDate given, Double temp, Double weight, Double height, String remarks, String by) {
    ImmunizationSchedule s = get(id);
    if (s.getStatus() == Status.VACCINATED) throw new BusinessException("This vaccine was already recorded as given.");
    LocalDate g = given != null ? given : LocalDate.now();
    if (g.isAfter(LocalDate.now())) throw new BusinessException("Date given cannot be in the future.");
    if (g.isBefore(s.getChild().getBirthDate())) throw new BusinessException("Date given cannot be before the child's birth date.");
    if (temp != null && (temp < 30 || temp > 45)) throw new BusinessException("Temperature must be between 30 and 45 \u00b0C.");
    if (height != null && (height < 20 || height > 200)) throw new BusinessException("Height must be between 20 and 200 cm.");
    if (weight != null && (weight < 0.5 || weight > 150)) throw new BusinessException("Weight must be between 0.5 and 150 kg.");
    if (remarks != null && remarks.length() > 500) throw new BusinessException("Remarks are too long (500 characters maximum).");
    s.setStatus(Status.VACCINATED); s.setDateGiven(g);
    if (weight != null) s.getChild().setWeight(weight); if (height != null) s.getChild().setHeight(height); s.setTemperature(temp); s.setWeight(weight); s.setHeight(height); s.setRemarks(remarks); s.setAdministeredBy(by);
    notifier.send(parentOf(s), "Vaccination recorded", s.getChild().getFullName() + " received " + s.getLabel() + " on " + Fmt.d(s.getDateGiven()) + ".");
  }

  public void absent(Long id, LocalDate newDate, String remarks) {
    ImmunizationSchedule s = get(id);
    if (s.getStatus() == Status.VACCINATED) throw new BusinessException("This vaccine was already given.");
    if (newDate != null && newDate.isBefore(LocalDate.now())) throw new BusinessException("The new date cannot be in the past.");
    if (remarks != null && remarks.length() > 500) throw new BusinessException("Remarks are too long (500 characters maximum).");
    s.setAbsentCount(s.getAbsentCount() + 1); s.setRemarks(remarks);
    if (newDate != null) { s.setScheduledDate(newDate); s.setStatus(Status.SCHEDULED); } else s.setStatus(Status.ABSENT);
    notifier.send(parentOf(s), "Missed vaccination", s.getChild().getFullName() + " was not present for " + s.getLabel() + "."
      + (newDate != null ? " New schedule: " + Fmt.d(newDate) + "." : " Please visit the health center for a new schedule."));
  }

  public void reschedule(Long id, LocalDate date, String why) {
    ImmunizationSchedule s = get(id);
    if (s.getStatus() == Status.VACCINATED) throw new BusinessException("This vaccine was already given, so it cannot be rescheduled.");
    if (date == null || date.isBefore(LocalDate.now())) throw new BusinessException("Please choose a date that is today or later.");
    s.setScheduledDate(date); s.setStatus(Status.SCHEDULED);
    notifier.send(parentOf(s), "Schedule updated", s.getChild().getFullName() + " - " + s.getLabel() + " is now on " + Fmt.d(date) + (why == null || why.isBlank() ? "." : ". " + why));
  }

  public void requestReschedule(Long id, User parent, LocalDate preferred, String reason) {
    ImmunizationSchedule s = get(id);
    if (s.getStatus() == Status.VACCINATED) throw new BusinessException("This vaccine was already given.");
    if (s.getStatus() == Status.RESCHEDULE_REQUESTED) throw new BusinessException("A request for this vaccine is already waiting for the midwife.");
    if (preferred == null || preferred.isBefore(LocalDate.now())) throw new BusinessException("Preferred date cannot be in the past.");
    if (reason == null || reason.isBlank() || reason.length() > 500) throw new BusinessException("Please give a short reason (500 characters maximum).");
    RescheduleRequest r = new RescheduleRequest(); r.setSchedule(s); r.setChild(s.getChild()); r.setVaccine(s.getVaccine()); r.setParent(parent); r.setPreferredDate(preferred); r.setReason(reason);
    s.getRequests().add(r); s.setStatus(Status.RESCHEDULE_REQUESTED);
    notifier.sendToRole(User.Role.MIDWIFE, "Reschedule request", parent.getFullName() + " asked to move " + s.getChild().getFullName() + " (" + s.getLabel() + ") to " + Fmt.d(preferred) + ".");
  }

  /** Registers a child, builds the full schedule and alerts midwives - all in one transaction (all or nothing). */
  public Child registerChild(Child c, User parent) {
    String name = c.getFullName().trim();
    if (children.findByParentOrderByFullName(parent).stream().anyMatch(x -> x.getFullName().trim().equalsIgnoreCase(name) && x.getBirthDate().equals(c.getBirthDate())))
      throw new BusinessException(name + " is already registered under your account.");
    c.setParent(parent); generate(c); children.save(c);
    notifier.sendToRole(User.Role.MIDWIFE, "New child registered", name + " (" + c.getAgeText() + ") was registered by " + parent.getFullName() + ".");
    return c;
  }

  public ImmunizationSchedule createVisit(Long childId, Long vaccineId, int dose, LocalDate date) {
    Child c = children.findById(childId).orElseThrow(() -> new BusinessException("Please choose a child."));
    Vaccine v = vaccines.findById(vaccineId).orElseThrow(() -> new BusinessException("Please choose a vaccine."));
    if (dose < 1 || dose > v.getTotalDoses()) throw new BusinessException("Dose number for " + v.getName() + " must be between 1 and " + v.getTotalDoses() + ".");
    if (date.isBefore(c.getBirthDate())) throw new BusinessException("The visit cannot be before the child's birth date.");
    if (c.getSchedules().stream().anyMatch(x -> x.getVaccine().getId().equals(v.getId()) && x.getDoseNumber() == dose)) throw new BusinessException("That dose is already on this child's schedule.");
    ImmunizationSchedule s = new ImmunizationSchedule(); s.setChild(c); s.setVaccine(v); s.setDoseNumber(dose); s.setScheduledDate(date);
    c.getSchedules().add(s); schedules.save(s);
    notifier.send(c.getParent(), "New visit scheduled", c.getFullName() + " - " + s.getLabel() + " on " + Fmt.d(date) + ".");
    return s;
  }

  public void requestVaccine(Long childId, Long vaccineId, User parent, LocalDate preferred, String note) {
    Child c = children.findById(childId).orElseThrow(); Vaccine v = vaccines.findById(vaccineId).orElseThrow();
    if (preferred == null || preferred.isBefore(LocalDate.now())) throw new BusinessException("Preferred date cannot be in the past.");
    if (note != null && note.length() > 500) throw new BusinessException("The note is too long (500 characters maximum).");
    if (c.getRequests().stream().anyMatch(x -> x.getStatus() == RescheduleRequest.Status.PENDING && x.getSchedule() == null && x.getVaccine().getId().equals(v.getId())))
      throw new BusinessException("You already requested " + v.getName() + " for " + c.getFullName() + ". Please wait for the midwife.");
    long given = c.getSchedules().stream().filter(x -> x.getVaccine().getId().equals(v.getId()) && x.getStatus() == Status.VACCINATED).count();
    if (given >= v.getTotalDoses()) throw new BusinessException(c.getFullName() + " already completed all doses of " + v.getName() + ".");
    RescheduleRequest r = new RescheduleRequest(); r.setChild(c); r.setVaccine(v); r.setParent(parent); r.setPreferredDate(preferred);
    r.setReason(note == null || note.isBlank() ? "Vaccine request" : note); c.getRequests().add(r);
    notifier.sendToRole(User.Role.MIDWIFE, "Vaccine request", parent.getFullName() + " requested " + v.getName() + " for " + c.getFullName() + " on " + Fmt.d(preferred) + ".");
  }

  public void decide(Long requestId, boolean approve, LocalDate date, String response) {
    RescheduleRequest r = requests.findById(requestId).orElseThrow(); ImmunizationSchedule s = r.getSchedule();
    if (r.getStatus() != RescheduleRequest.Status.PENDING) throw new BusinessException("This request was already answered.");
    if (approve && (date != null ? date : r.getPreferredDate()).isBefore(LocalDate.now())) throw new BusinessException("The approved date cannot be in the past.");
    if (response != null && response.length() > 500) throw new BusinessException("The reply is too long (500 characters maximum).");
    r.setResponse(response); r.setStatus(approve ? RescheduleRequest.Status.APPROVED : RescheduleRequest.Status.REJECTED);
    LocalDate d = date != null ? date : r.getPreferredDate();
    if (s == null) {
      Child c = r.getChild(); Vaccine v = r.getVaccine();
      if (approve) {
        s = c.getSchedules().stream().filter(x -> x.getVaccine().getId().equals(v.getId()) && x.getStatus() != Status.VACCINATED).findFirst().orElse(null);
        if (s == null) {
          s = new ImmunizationSchedule(); s.setChild(c); s.setVaccine(v);
          s.setDoseNumber((int) c.getSchedules().stream().filter(x -> x.getVaccine().getId().equals(v.getId())).count() + 1); c.getSchedules().add(s);
        }
        s.setScheduledDate(d); s.setStatus(Status.SCHEDULED);
      }
      notifier.send(r.getParent(), approve ? "Vaccine request approved" : "Vaccine request declined",
        c.getFullName() + " - " + v.getName() + (approve ? " is scheduled on " + Fmt.d(d) + "." : " was not scheduled. " + (response == null ? "" : response)));
      return;
    }
    if (approve) s.setScheduledDate(d);
    s.setStatus(Status.SCHEDULED);
    notifier.send(r.getParent(), approve ? "Reschedule approved" : "Reschedule declined",
      s.getChild().getFullName() + " - " + s.getLabel() + (approve ? " is now on " + Fmt.d(s.getScheduledDate()) + "." : " stays on " + Fmt.d(s.getScheduledDate()) + ". " + (response == null ? "" : response)));
  }

  /** Disaster / emergency: move every pending schedule in [from, to] to newDate and notify each parent once. */
  public int massReschedule(LocalDate from, LocalDate to, LocalDate newDate, String reason, String msg, String by) {
    List<ImmunizationSchedule> list = schedules.findByStatusInAndScheduledDateBetween(List.of(Status.SCHEDULED, Status.RESCHEDULE_REQUESTED), from, to);
    Map<User, Set<String>> byParent = new LinkedHashMap<>();
    for (ImmunizationSchedule s : list) {
      s.setScheduledDate(newDate); s.setStatus(Status.SCHEDULED);
      s.getRequests().stream().filter(r -> r.getStatus() == RescheduleRequest.Status.PENDING)
        .forEach(r -> { r.setStatus(RescheduleRequest.Status.APPROVED); r.setResponse("Moved by the health office: " + reason); });
      byParent.computeIfAbsent(parentOf(s), k -> new LinkedHashSet<>()).add(s.getChild().getFullName());
    }
    byParent.forEach((p, kids) -> notifier.send(p, "Schedule moved: " + reason,
      (msg == null || msg.isBlank() ? "" : msg + " ") + "New date for " + String.join(", ", kids) + ": " + Fmt.d(newDate) + "."));
    EmergencyEvent e = new EmergencyEvent(); e.setReason(reason); e.setMessage(msg); e.setFromDate(from); e.setToDate(to);
    e.setNewDate(newDate); e.setCreatedBy(by); e.setAffected(list.size()); emergencies.save(e);
    return list.size();
  }
}
