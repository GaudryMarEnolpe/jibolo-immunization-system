package com.jibolo.immunization.controller;
import com.jibolo.immunization.model.User; import com.jibolo.immunization.repository.*;
import lombok.RequiredArgsConstructor; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import java.security.Principal;
@ControllerAdvice @RequiredArgsConstructor
public class GlobalAdvice {
  private final UserRepository users; private final NotificationRepository notes;
  @ModelAttribute void common(Model m, Principal p) {
    if (p == null) return;
    users.findByUsername(p.getName()).ifPresent(u -> { m.addAttribute("me", u); m.addAttribute("unread", notes.countByUserAndSeenFalse(u)); });
  }
}
