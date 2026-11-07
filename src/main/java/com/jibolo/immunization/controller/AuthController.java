package com.jibolo.immunization.controller;
import com.jibolo.immunization.model.User; import com.jibolo.immunization.service.*;
import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
@Controller @RequiredArgsConstructor
public class AuthController {
  private final UserService users;
  @Value("${app.staff-code}") private String code;
  @GetMapping("/login") String login() { return "login"; }
  @GetMapping("/register") String register() { return "register"; }
  @PostMapping("/register")
  String doRegister(@RequestParam String fullName, @RequestParam String username, @RequestParam String password, @RequestParam String confirm,
                    @RequestParam(required = false) String email, @RequestParam(required = false) String contact,
                    @RequestParam(defaultValue = "PARENT") User.Role role, @RequestParam(required = false) String staffCode, RedirectAttributes ra) {
    String err = Validation.account(fullName, username, password, email, contact);
    if (err == null && users.exists(username.trim())) err = "That username is already taken.";
    else if (err == null && !password.equals(confirm)) err = "Passwords do not match.";
    else if (err == null && role != User.Role.PARENT && !sameCode(staffCode)) err = "Invalid staff registration code.";
    if (err != null) { ra.addFlashAttribute("error", err); return "redirect:/register"; }
    users.create(fullName, username, password, blankToNull(email), blankToNull(contact), role);
    ra.addFlashAttribute("success", "Account created. You can now log in.");
    return "redirect:/login";
  }
  private boolean sameCode(String given) { return given != null && MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8), given.trim().getBytes(StandardCharsets.UTF_8)); }
  private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
