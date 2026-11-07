package com.jibolo.immunization.service;
import com.jibolo.immunization.model.*; import com.jibolo.immunization.repository.*;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class NotificationService {
  private final NotificationRepository repo; private final UserRepository users;
  public void send(User u, String title, String msg) { if (u == null) return; Notification n = new Notification(); n.setUser(u); n.setTitle(title); n.setMessage(msg); repo.save(n); }
  public void sendToRole(User.Role r, String title, String msg) { users.findByRole(r).forEach(u -> send(u, title, msg)); }
}
