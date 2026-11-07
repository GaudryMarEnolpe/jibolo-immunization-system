package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
  public enum Role { PARENT, MIDWIFE, BHW }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String username;
  private String password;
  private String fullName;
  private String email;
  private String contact;
  @Enumerated(EnumType.STRING) private Role role;
  private boolean active = true;
  public String getRoleLabel() { return role == Role.BHW ? "Barangay Health Worker" : role == Role.MIDWIFE ? "Midwife" : "Parent / Guardian"; }
}
