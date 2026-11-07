package com.jibolo.immunization.service;
import org.springframework.stereotype.Service;
import java.time.*; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
/** Locks an account for 15 minutes after 5 wrong passwords (slows down password guessing). */
@Service
public class LoginAttemptService {
  private static final int MAX = 5; private static final Duration LOCK = Duration.ofMinutes(15);
  private record State(int fails, Instant lockedUntil) {}
  private final Map<String, State> map = new ConcurrentHashMap<>();
  private String key(String u) { return u == null ? "" : u.toLowerCase(); }
  public boolean isLocked(String u) { State s = map.get(key(u)); return s != null && s.lockedUntil() != null && Instant.now().isBefore(s.lockedUntil()); }
  public void failed(String u) {
    map.merge(key(u), new State(1, null), (o, n) -> {
      int f = (o.lockedUntil() != null && !Instant.now().isBefore(o.lockedUntil())) ? 1 : o.fails() + 1;
      return new State(f, f >= MAX ? Instant.now().plus(LOCK) : null); });
  }
  public void succeeded(String u) { map.remove(key(u)); }
}
