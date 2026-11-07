package com.jibolo.immunization.model;
import jakarta.persistence.*; import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor
public class Vaccine {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true) private String code;
  private String name;
  @Column(length = 300) private String description;
  private int totalDoses = 1;
}
