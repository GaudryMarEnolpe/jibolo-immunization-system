package com.jibolo.immunization.controller;
import com.jibolo.immunization.config.SecurityConfig; import com.jibolo.immunization.model.*; import com.jibolo.immunization.repository.*; import com.jibolo.immunization.service.*;
import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal; import java.util.List;

/** Pages shared by all three roles: child record, printable card, notifications, account settings. */
@Controller @RequiredArgsConstructor
public class CommonController {
  private final UserService users; private final ChildRepository children; private final NotificationRepository notes;

  @GetMapping("/") String home(Authentication a) { return "redirect:" + SecurityConfig.homeFor(a); }

  private Child find(Long id, Principal p) {
    Child c = children.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    User me = users.byUsername(p.getName());
    if (me.getRole() == User.Role.PARENT && !c.getParent().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return c;
  }
  @GetMapping("/child/{id}") String child(@PathVariable Long id, Principal p, Model m) { m.addAttribute("child", find(id, p)); return "child-detail"; }
  @GetMapping("/child/{id}/card") String card(@PathVariable Long id, Principal p, Model m) { m.addAttribute("child", find(id, p)); return "card"; }

  /** Midwife / BHW record height and weight; BMI is calculated automatically when the record is saved. */
  @PostMapping("/child/{id}/measure")
  String measure(@PathVariable Long id, @RequestParam Double height, @RequestParam Double weight, RedirectAttributes ra) {
    String err = Validation.measure(height, weight); if (err != null) { ra.addFlashAttribute("error", err); return "redirect:/child/" + id; }
    Child c = children.findById(id).orElseThrow(); c.setHeight(height); c.setWeight(weight); c = children.saveAndFlush(c);
    ra.addFlashAttribute("success", "Height and weight saved. BMI is " + c.getBmi() + "."); return "redirect:/child/" + id;
  }

  @GetMapping("/notifications") String list(Principal p, Model m) { m.addAttribute("notes", notes.findByUserOrderByCreatedAtDesc(users.byUsername(p.getName()))); return "notifications"; }
  @PostMapping("/notifications/read") String read(Principal p) {
    List<Notification> l = notes.findByUserOrderByCreatedAtDesc(users.byUsername(p.getName())); l.forEach(n -> n.setSeen(true)); notes.saveAll(l); return "redirect:/notifications";
  }

  @GetMapping("/account") String account() { return "account"; }
  @PostMapping("/account/profile")
  String profile(@RequestParam String fullName, @RequestParam(required = false) String email, @RequestParam(required = false) String contact, Principal p, RedirectAttributes ra) {
    String err = Validation.profile(fullName, email, contact);
    if (err != null) { ra.addFlashAttribute("error", err); return "redirect:/account"; }
    User u = users.byUsername(p.getName()); u.setFullName(fullName.trim()); u.setEmail(email); u.setContact(contact); users.save(u);
    ra.addFlashAttribute("success", "Profile updated."); return "redirect:/account";
  }
  @PostMapping("/account/password")
  String password(@RequestParam String current, @RequestParam String password, @RequestParam String confirm, Principal p, RedirectAttributes ra) {
    User u = users.byUsername(p.getName());
    if (!users.matches(current, u)) ra.addFlashAttribute("error", "Current password is incorrect.");
    else if (Validation.password(password) != null) ra.addFlashAttribute("error", Validation.password(password));
    else if (!password.equals(confirm)) ra.addFlashAttribute("error", "New passwords do not match.");
    else { users.setPassword(u, password); ra.addFlashAttribute("success", "Password changed."); }
    return "redirect:/account";
  }
}
