package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor
public class Notification {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional = false) private User user;
  private String title;
  @Column(length = 600) private String message;
  private boolean seen;
  private LocalDateTime createdAt = LocalDateTime.now();
  public String getTimeText() { return Fmt.dt(createdAt); }
}
