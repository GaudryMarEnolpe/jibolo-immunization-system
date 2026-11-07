package com.jibolo.immunization.service;
import com.jibolo.immunization.model.User; import com.jibolo.immunization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails; import org.springframework.security.core.userdetails.UserDetailsService; import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class UserService implements UserDetailsService {
  private final UserRepository repo; private final PasswordEncoder enc; private final LoginAttemptService attempts;
  @Override public UserDetails loadUserByUsername(String username) {
    User x = repo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
    return org.springframework.security.core.userdetails.User.withUsername(x.getUsername()).password(x.getPassword()).roles(x.getRole().name()).disabled(!x.isActive()).accountLocked(attempts.isLocked(x.getUsername())).build();
  }
  public User byUsername(String u) { return repo.findByUsername(u).orElseThrow(); }
  public boolean exists(String u) { return repo.existsByUsername(u); }
  public User save(User u) { return repo.save(u); }
  public User create(String fullName, String username, String password, String email, String contact, User.Role role) {
    User u = new User(); u.setFullName(fullName.trim()); u.setUsername(username.trim()); u.setPassword(enc.encode(password));
    u.setEmail(email); u.setContact(contact); u.setRole(role); return repo.save(u);
  }
  public boolean matches(String raw, User u) { return enc.matches(raw, u.getPassword()); }
  public void setPassword(User u, String raw) { u.setPassword(enc.encode(raw)); repo.save(u); }
}
