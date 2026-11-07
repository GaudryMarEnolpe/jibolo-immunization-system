package com.jibolo.immunization.controller;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.model.ImmunizationSchedule.Status; import com.jibolo.immunization.repository.*; import com.jibolo.immunization.service.*;
import jakarta.servlet.http.HttpServletRequest; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Controller; import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal; import java.time.LocalDate;

@Controller @RequestMapping("/midwife") @RequiredArgsConstructor
public class MidwifeController {
  private final UserService users; private final ScheduleRepository schedules; private final ChildRepository children; private final VaccineRepository vaccines;
  private final RequestRepository requests; private final ScheduleService svc; private final NotificationService notifier;

  private String back(HttpServletRequest r) { String ref = r.getHeader("Referer"); return "redirect:" + ErrorAdvice.safePath(ref, "/midwife/schedules"); }

  @GetMapping("/dashboard")
  String dash(Model m) {
    LocalDate today = LocalDate.now(); var all = schedules.findAllByOrderByScheduledDateAsc();
    m.addAttribute("todayList", all.stream().filter(s -> today.equals(s.getScheduledDate()) && s.getStatus() != Status.VACCINATED).toList());
    m.addAttribute("overdue", all.stream().filter(ImmunizationSchedule::isOverdue).count());
    m.addAttribute("next7", all.stream().filter(s -> s.getStatus() == Status.SCHEDULED && s.getScheduledDate().isAfter(today) && s.getScheduledDate().isBefore(today.plusDays(8))).count());
    m.addAttribute("pending", requests.findByStatusOrderByCreatedAtAsc(RescheduleRequest.Status.PENDING).size());
    m.addAttribute("vaccinatedToday", all.stream().filter(s -> today.equals(s.getDateGiven())).count());
    m.addAttribute("asOf", "As of " + Fmt.d(today)); return "midwife/dashboard";
  }

  @GetMapping("/schedules")
  String list(@RequestParam(required = false) String q, @RequestParam(required = false) LocalDate date, @RequestParam(required = false) String status, Model m) {
    String k = q == null ? "" : q.trim().toLowerCase();
    var list = schedules.findAllByOrderByScheduledDateAsc().stream()
      .filter(s -> k.isEmpty() || s.getChild().getFullName().toLowerCase().contains(k) || String.valueOf(s.getChild().getMotherName()).toLowerCase().contains(k))
      .filter(s -> date == null || date.equals(s.getScheduledDate()))
      .filter(s -> status == null || status.isBlank() ? s.getStatus() != Status.VACCINATED : status.equals("ALL") || (status.equals("OVERDUE") ? s.isOverdue() : s.getStatus().name().equals(status)))
      .toList();
    m.addAttribute("list", list); m.addAttribute("q", q); m.addAttribute("date", date); m.addAttribute("status", status); return "midwife/schedules";
  }

  @GetMapping("/schedules/new")
  String newForm(Model m) { m.addAttribute("children", children.findAll()); m.addAttribute("vaccines", vaccines.findAll()); return "midwife/schedule-form"; }

  @PostMapping("/schedules")
  String create(@RequestParam Long childId, @RequestParam Long vaccineId, @RequestParam int dose, @RequestParam LocalDate date, RedirectAttributes ra) {
    svc.createVisit(childId, vaccineId, dose, date); ra.addFlashAttribute("success", "Visit scheduled and parent notified."); return "redirect:/midwife/schedules";
  }

  @PostMapping("/schedules/{id}/vaccinate")
  String vaccinate(@PathVariable Long id, @RequestParam(required = false) LocalDate given, @RequestParam(required = false) Double temperature, @RequestParam(required = false) Double weight,
                   @RequestParam(required = false) Double height, @RequestParam(required = false) String remarks, Principal p, HttpServletRequest req, RedirectAttributes ra) {
    svc.vaccinate(id, given, temperature, weight, height, remarks, users.byUsername(p.getName()).getFullName());
    ra.addFlashAttribute("success", "Vaccination recorded."); return back(req);
  }

  @PostMapping("/schedules/{id}/absent")
  String absent(@PathVariable Long id, @RequestParam(required = false) LocalDate newDate, @RequestParam(required = false) String remarks, HttpServletRequest req, RedirectAttributes ra) {
    svc.absent(id, newDate, remarks); ra.addFlashAttribute("success", "Marked as not present; parent notified."); return back(req);
  }

  @PostMapping("/schedules/{id}/reschedule")
  String reschedule(@PathVariable Long id, @RequestParam LocalDate date, @RequestParam(required = false) String why, HttpServletRequest req, RedirectAttributes ra) {
    svc.reschedule(id, date, why); ra.addFlashAttribute("success", "Schedule updated; parent notified."); return back(req);
  }

  @PostMapping("/schedules/{id}/delete")
  String delete(@PathVariable Long id, HttpServletRequest req, RedirectAttributes ra) {
    if (schedules.findById(id).orElseThrow().getStatus() == Status.VACCINATED) throw new BusinessException("Vaccinations that were already given are official records and cannot be deleted.");
    schedules.deleteById(id); ra.addFlashAttribute("success", "Schedule entry deleted."); return back(req); }

  @GetMapping("/requests")
  String reqs(Model m) { m.addAttribute("pending", requests.findByStatusOrderByCreatedAtAsc(RescheduleRequest.Status.PENDING)); return "midwife/requests"; }

  @PostMapping("/requests/{id}/approve")
  String approve(@PathVariable Long id, @RequestParam(required = false) LocalDate date, RedirectAttributes ra) { svc.decide(id, true, date, null); ra.addFlashAttribute("success", "Request approved."); return "redirect:/midwife/requests"; }

  @PostMapping("/requests/{id}/reject")
  String reject(@PathVariable Long id, @RequestParam(required = false) String response, RedirectAttributes ra) { svc.decide(id, false, null, response); ra.addFlashAttribute("success", "Request declined."); return "redirect:/midwife/requests"; }

  @GetMapping("/children")
  String kids(@RequestParam(required = false) String q, Model m) {
    m.addAttribute("list", q == null || q.isBlank() ? children.findAll() : children.search("%" + q.trim().toLowerCase() + "%")); m.addAttribute("q", q); m.addAttribute("base", "/midwife"); return "children";
  }
}
