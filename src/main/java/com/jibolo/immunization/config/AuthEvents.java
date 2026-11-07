package com.jibolo.immunization.config;
import com.jibolo.immunization.service.LoginAttemptService; import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener; import org.springframework.security.authentication.event.*; import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AuthEvents {
  private final LoginAttemptService attempts;
  @EventListener public void bad(AuthenticationFailureBadCredentialsEvent e) { attempts.failed(e.getAuthentication().getName()); }
  @EventListener public void ok(AuthenticationSuccessEvent e) { attempts.succeeded(e.getAuthentication().getName()); }
}
