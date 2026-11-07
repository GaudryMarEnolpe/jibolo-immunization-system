package com.jibolo.immunization.controller;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.model.ImmunizationSchedule.Status; import com.jibolo.immunization.repository.*;
import com.jibolo.immunization.service.*;
import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Controller; import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal; import java.time.LocalDate;

@Controller @RequestMapping("/parent") @RequiredArgsConstructor
public class ParentController {
  private final UserService users; private final ChildRepository children; private final ScheduleRepository schedules;
  private final VaccineRepository vaccines; private final ScheduleService svc; private final NotificationService notifier;

  private Child own(Long id, Principal p) {
    Child c = children.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!c.getParent().getUsername().equals(p.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return c;
  }

  @GetMapping("/dashboard")
  String dash(Principal p, Model m) {
    User me = users.byUsername(p.getName());
    m.addAttribute("children", children.findByParentOrderByFullName(me));
    m.addAttribute("upcoming", schedules.findByChildParentOrderByScheduledDateAsc(me).stream().filter(s -> s.getStatus() != Status.VACCINATED).toList());
    m.addAttribute("vaccines", vaccines.findAll()); return "parent/dashboard";
  }

  @GetMapping("/children/new")
  String form(Principal p, Model m) {
    Child c = new Child(); c.setMotherName(users.byUsername(p.getName()).getFullName());
    m.addAttribute("child", c); m.addAttribute("action", "/parent/children"); m.addAttribute("edit", false); return "parent/child-form";
  }

  private String formError(Model m, Child f, String err, String action, boolean edit) {
    m.addAttribute("child", f); m.addAttribute("error", err); m.addAttribute("action", action); m.addAttribute("edit", edit); return "parent/child-form";
  }

  @PostMapping("/children")
  String create(@ModelAttribute Child form, Principal p, Model m, RedirectAttributes ra) {
    String err = !form.isConsent() ? "Parent/guardian consent for immunization is required to register a child." : Validation.child(form, false);
    if (err != null) return formError(m, form, err, "/parent/children", false);   // keeps what the parent typed
    Child c = new Child(); copy(form, c); c.setBirthDate(form.getBirthDate()); c.setConsent(true); c.setConsentDate(LocalDate.now());
    svc.registerChild(c, users.byUsername(p.getName()));
    ra.addFlashAttribute("success", "Child registered. The immunization schedule was created automatically.");
    return "redirect:/child/" + c.getId();
  }

  private void copy(Child f, Child c) {
    c.setFullName(f.getFullName().trim()); c.setSex(f.getSex()); c.setMotherName(f.getMotherName()); c.setFatherName(f.getFatherName());
    c.setPurok(f.getPurok()); c.setBirthWeight(f.getBirthWeight()); c.setPlaceOfDelivery(f.getPlaceOfDelivery());
  }

  @GetMapping("/children/{id}/edit")
  String edit(@PathVariable Long id, Principal p, Model m) { m.addAttribute("child", own(id, p)); m.addAttribute("action", "/parent/children/" + id); m.addAttribute("edit", true); return "parent/child-form"; }

  @PostMapping("/children/{id}")
  String update(@PathVariable Long id, @ModelAttribute Child form, Principal p, Model m, RedirectAttributes ra) {
    Child c = own(id, p); String err = Validation.child(form, true);
    if (err != null) return formError(m, form, err, "/parent/children/" + id, true);
    copy(form, c); children.save(c); ra.addFlashAttribute("success", "Child information updated."); return "redirect:/child/" + id;
  }

  @PostMapping("/children/{id}/delete")
  String delete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
    Child c = own(id, p);
    if (c.getDone() > 0) { ra.addFlashAttribute("error", "This child already has vaccinations on record, so a parent cannot delete it. Please ask the barangay health worker."); return "redirect:/child/" + id; }
    children.delete(c); ra.addFlashAttribute("success", "Child record deleted."); return "redirect:/parent/dashboard"; }

  @PostMapping("/vaccine-request")
  String vaccineRequest(@RequestParam Long childId, @RequestParam Long vaccineId, @RequestParam LocalDate preferred, @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
    Child c = own(childId, p);
    if (preferred.isBefore(LocalDate.now())) { ra.addFlashAttribute("error", "Preferred date cannot be in the past."); return "redirect:/parent/dashboard"; }
    svc.requestVaccine(c.getId(), vaccineId, users.byUsername(p.getName()), preferred, note);
    ra.addFlashAttribute("success", "Vaccine request sent to the midwife."); return "redirect:/parent/dashboard";
  }

  @GetMapping("/reschedule/{sid}")
  String rform(@PathVariable Long sid, Principal p, Model m) {
    ImmunizationSchedule s = schedules.findById(sid).orElseThrow(); own(s.getChild().getId(), p); m.addAttribute("s", s); return "parent/reschedule-form";
  }

  @PostMapping("/reschedule/{sid}")
  String request(@PathVariable Long sid, @RequestParam LocalDate preferred, @RequestParam String reason, Principal p, RedirectAttributes ra) {
    ImmunizationSchedule s = schedules.findById(sid).orElseThrow(); own(s.getChild().getId(), p);
    if (preferred.isBefore(LocalDate.now())) { ra.addFlashAttribute("error", "Preferred date cannot be in the past."); return "redirect:/parent/reschedule/" + sid; }
    svc.requestReschedule(sid, users.byUsername(p.getName()), preferred, reason);
    ra.addFlashAttribute("success", "Reschedule request sent. You will be notified once the midwife responds."); return "redirect:/parent/dashboard";
  }
}
