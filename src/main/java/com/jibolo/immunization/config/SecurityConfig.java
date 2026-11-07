package com.jibolo.immunization.config;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain; import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfig {
  public static String homeFor(Authentication a) {
    if (a == null) return "/login";
    String r = a.getAuthorities().iterator().next().getAuthority();
    return r.equals("ROLE_PARENT") ? "/parent/dashboard" : r.equals("ROLE_MIDWIFE") ? "/midwife/dashboard" : "/bhw/dashboard";
  }
  private static final String CSP = "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
    + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; font-src 'self' data: https://cdn.jsdelivr.net https://fonts.gstatic.com; "
    + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'; object-src 'none'";

  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

  @Bean SecurityFilterChain chain(HttpSecurity http, @Value("${app.require-https:false}") boolean https) throws Exception {
    if (https) http.requiresChannel(c -> c.anyRequest().requiresSecure());
    http.authorizeHttpRequests(a -> a
        .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**", "/actuator/health").permitAll()
        .requestMatchers("/child/*/measure").hasAnyRole("MIDWIFE", "BHW")
        .requestMatchers("/parent/**").hasRole("PARENT")      // parents can only see parent pages
        .requestMatchers("/midwife/**").hasRole("MIDWIFE")    // midwife pages are closed to parents and BHW
        .requestMatchers("/bhw/**").hasRole("BHW")            // BHW pages are closed to parents and midwives
        .anyRequest().authenticated())                        // everything else needs a login (default = deny anonymous)
      .formLogin(f -> f.loginPage("/login").successHandler((req, res, auth) -> res.sendRedirect(req.getContextPath() + homeFor(auth))).permitAll())
      .logout(l -> l.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll())
      .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
      .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(CSP))
        .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        .frameOptions(f -> f.deny())
        .httpStrictTransportSecurity(x -> x.includeSubDomains(true).maxAgeInSeconds(31536000)));
    return http.build();
  }
}
