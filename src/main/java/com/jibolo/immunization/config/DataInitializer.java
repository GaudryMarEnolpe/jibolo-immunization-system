package com.jibolo.immunization.config;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.repository.*; import com.jibolo.immunization.service.*;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.CommandLineRunner; import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @Slf4j
public class DataInitializer implements CommandLineRunner {
  private final VaccineRepository vaccines; private final UserRepository userRepo; private final UserService users;
  @Value("${app.bootstrap.bhw-password:}") private String bhwPw;
  @Value("${app.bootstrap.midwife-password:}") private String midwifePw;
  private void v(String code, String name, String desc, int doses) { Vaccine x = new Vaccine(); x.setCode(code); x.setName(name); x.setDescription(desc); x.setTotalDoses(doses); vaccines.save(x); }
  @Override public void run(String... args) {
    if (vaccines.count() == 0) {
      v("BCG", "BCG", "At birth", 1); v("HEPB", "Hepatitis B", "Within 24 hours of birth", 1);
      v("PENTA", "Pentavalent (DPT-HepB-Hib)", "6, 10, 14 weeks", 3); v("OPV", "Oral Polio Vaccine (OPV)", "6, 10, 14 weeks", 3);
      v("PCV", "Pneumococcal Vaccine (PCV)", "6, 10, 14 weeks", 3); v("IPV", "Inactivated Polio Vaccine (IPV)", "14 weeks and 9 months", 2);
      v("MMR", "Measles-Rubella (MR/MMR)", "9 and 12 months", 2);
    }
    if (userRepo.count() == 0) {
      if (!bhwPw.isBlank()) { users.create("Barangay Health Worker", "bhw", bhwPw, null, null, User.Role.BHW); log.info("First BHW account 'bhw' created."); }
      else log.warn("No users exist and BOOTSTRAP_BHW_PASSWORD is not set - nobody can log in. Set it for the first start.");
      if (!midwifePw.isBlank()) users.create("Jibolo Midwife", "midwife", midwifePw, null, null, User.Role.MIDWIFE);
    }
  }
}
