package com.jibolo.immunization.controller;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.model.ImmunizationSchedule.Status; import com.jibolo.immunization.repository.*; import com.jibolo.immunization.service.*;
import lombok.RequiredArgsConstructor; import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.*; import org.springframework.stereotype.Controller; import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal; import java.time.LocalDate; import java.util.*; import java.util.stream.Collectors;

@Controller @RequestMapping("/bhw") @RequiredArgsConstructor
public class BhwController {
  public record Row(String name, long total, long done, long overdue) { public int pct() { return total == 0 ? 0 : (int) (done * 100 / total); } }

  private final UserService users; private final UserRepository userRepo; private final ChildRepository children; private final ScheduleRepository schedules;
  private final EmergencyRepository emergencies; private final ScheduleService svc;

  private List<ImmunizationSchedule> filtered(LocalDate from, LocalDate to) {
    return schedules.findAllByOrderByScheduledDateAsc().stream().filter(s -> (from == null || !s.getScheduledDate().isBefore(from)) && (to == null || !s.getScheduledDate().isAfter(to))).toList();
  }

  @GetMapping("/dashboard")
  String dash(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Model m) {
    var all = filtered(from, to); var kids = all.stream().map(ImmunizationSchedule::getChild).distinct().toList();
    m.addAttribute("totalChildren", kids.size()); m.addAttribute("fully", kids.stream().filter(Child::isFullyImmunized).count());
    m.addAttribute("doses", all.size()); m.addAttribute("vaccinated", all.stream().filter(s -> s.getStatus() == Status.VACCINATED).count());
    m.addAttribute("overdue", all.stream().filter(ImmunizationSchedule::isOverdue).count()); m.addAttribute("absent", all.stream().filter(s -> s.getStatus() == Status.ABSENT).count());
    m.addAttribute("perVaccine", all.stream().collect(Collectors.groupingBy(s -> s.getVaccine().getName(), LinkedHashMap::new, Collectors.toList()))
      .entrySet().stream().map(e -> new Row(e.getKey(), e.getValue().size(), e.getValue().stream().filter(s -> s.getStatus() == Status.VACCINATED).count(), e.getValue().stream().filter(ImmunizationSchedule::isOverdue).count())).toList());
    m.addAttribute("perPurok", kids.stream().collect(Collectors.groupingBy(c -> c.getPurok() == null || c.getPurok().isBlank() ? "Unspecified" : c.getPurok(), TreeMap::new, Collectors.toList()))
      .entrySet().stream().map(e -> new Row(e.getKey(), e.getValue().size(), e.getValue().stream().filter(Child::isFullyImmunized).count(), e.getValue().stream().filter(Child::isHasOverdue).count())).toList());
    m.addAttribute("asOf", from == null && to == null ? "As of " + Fmt.d(LocalDate.now()) : from == null ? "Up to " + Fmt.d(to) : to == null ? "From " + Fmt.d(from) : Fmt.d(from) + " - " + Fmt.d(to));
    m.addAttribute("from", from); m.addAttribute("to", to); return "bhw/dashboard";
  }

  private String q(String s) { String v = s == null ? "" : s; if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v; return "\"" + v.replace("\"", "\"\"") + "\""; }

  @GetMapping("/report.csv")
  ResponseEntity<String> csv(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
    StringBuilder sb = new StringBuilder("Child,Mother,Purok,Vaccine,Scheduled,Status,Date Given,Given By\n");
    for (ImmunizationSchedule s : filtered(from, to))
      sb.append(String.join(",", q(s.getChild().getFullName()), q(s.getChild().getMotherName()), q(s.getChild().getPurok()), q(s.getLabel()), q(s.getScheduledDate().toString()),
        q(s.getStatus().name()), q(s.getDateGiven() == null ? "" : s.getDateGiven().toString()), q(s.getAdministeredBy()))).append("\n");
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=immunization-report.csv").contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
  }

  @GetMapping("/children")
  String kids(@RequestParam(required = false) String q, Model m) {
    m.addAttribute("list", q == null || q.isBlank() ? children.findAll() : children.search("%" + q.trim().toLowerCase() + "%")); m.addAttribute("q", q); m.addAttribute("base", "/bhw"); return "children";
  }

  @PostMapping("/children/{id}/delete")
  String deleteChild(@PathVariable Long id, RedirectAttributes ra) { children.deleteById(id); ra.addFlashAttribute("success", "Child record deleted."); return "redirect:/bhw/children"; }

  @GetMapping("/emergency")
  String emergency(Model m) { m.addAttribute("events", emergencies.findAllByOrderByCreatedAtDesc()); m.addAttribute("today", LocalDate.now()); return "bhw/emergency"; }

  @PostMapping("/emergency")
  String doEmergency(@RequestParam String reason, @RequestParam LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam LocalDate newDate,
                     @RequestParam(required = false) String message, Principal p, RedirectAttributes ra) {
    LocalDate end = to == null ? from : to;
    if (end.isBefore(from)) { ra.addFlashAttribute("error", "The 'to' date cannot be before the 'from' date."); return "redirect:/bhw/emergency"; }
    if (reason.isBlank() || reason.length() > 100 || (message != null && message.length() > 500)) { ra.addFlashAttribute("error", "Please enter a short reason (100 characters) and a message of at most 500 characters."); return "redirect:/bhw/emergency"; }
    if (!newDate.isAfter(end)) { ra.addFlashAttribute("error", "The new date must be after the affected dates."); return "redirect:/bhw/emergency"; }
    if (newDate.isBefore(LocalDate.now())) { ra.addFlashAttribute("error", "The new date cannot be in the past."); return "redirect:/bhw/emergency"; }
    int n = svc.massReschedule(from, end, newDate, reason, message, users.byUsername(p.getName()).getFullName());
    ra.addFlashAttribute("success", n + " schedule(s) moved to " + Fmt.d(newDate) + ". Parents/guardians were notified automatically."); return "redirect:/bhw/emergency";
  }

  @GetMapping("/accounts")
  String accounts(Model m) { m.addAttribute("accounts", userRepo.findAll()); return "bhw/accounts"; }

  @PostMapping("/accounts")
  String add(@RequestParam String fullName, @RequestParam String username, @RequestParam String password, @RequestParam User.Role role,
             @RequestParam(required = false) String email, @RequestParam(required = false) String contact, RedirectAttributes ra) {
    if (users.exists(username.trim())) ra.addFlashAttribute("error", "That username is already taken.");
    else if (Validation.account(fullName, username, password, email, contact) != null) ra.addFlashAttribute("error", Validation.account(fullName, username, password, email, contact));
    else { users.create(fullName, username, password, email, contact, role); ra.addFlashAttribute("success", "Account added."); }
    return "redirect:/bhw/accounts";
  }

  @PostMapping("/accounts/{id}/toggle")
  String toggle(@PathVariable Long id, Principal p, RedirectAttributes ra) {
    User u = userRepo.findById(id).orElseThrow();
    if (u.getUsername().equals(p.getName())) ra.addFlashAttribute("error", "You cannot deactivate your own account.");
    else { u.setActive(!u.isActive()); userRepo.save(u); ra.addFlashAttribute("success", "Account " + (u.isActive() ? "activated." : "deactivated.")); }
    return "redirect:/bhw/accounts";
  }

  @PostMapping("/accounts/{id}/delete")
  String delete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
    User u = userRepo.findById(id).orElseThrow();
    if (u.getUsername().equals(p.getName())) { ra.addFlashAttribute("error", "You cannot delete your own account."); return "redirect:/bhw/accounts"; }
    try { userRepo.delete(u); userRepo.flush(); ra.addFlashAttribute("success", "Account deleted."); }
    catch (DataIntegrityViolationException e) { ra.addFlashAttribute("error", "This account has records (children/notifications). Deactivate it instead."); }
    return "redirect:/bhw/accounts";
  }
}
