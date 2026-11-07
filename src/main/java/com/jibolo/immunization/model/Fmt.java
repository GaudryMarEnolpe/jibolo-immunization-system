package com.jibolo.immunization.model;
import java.time.*; import java.time.format.DateTimeFormatter;
public class Fmt {
  static final DateTimeFormatter D = DateTimeFormatter.ofPattern("MMM d, yyyy");
  static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
  public static String d(LocalDate x) { return x == null ? "-" : x.format(D); }
  public static String dt(LocalDateTime x) { return x == null ? "-" : x.format(DT); }
}
