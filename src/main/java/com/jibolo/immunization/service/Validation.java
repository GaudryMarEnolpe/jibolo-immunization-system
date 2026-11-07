package com.jibolo.immunization.service;
import com.jibolo.immunization.model.Child;
import java.time.LocalDate; import java.util.regex.Pattern;
/** Server-side input checks. Each method returns an error message, or null when everything is valid. */
public final class Validation {
  private Validation() {}
  private static final Pattern USER = Pattern.compile("^[A-Za-z0-9._-]{4,30}$");
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");
  static boolean blank(String s) { return s == null || s.isBlank(); }

  public static String profile(String name, String email, String contact) {
    if (blank(name) || name.trim().length() < 2 || name.length() > 100) return "Please enter a full name (2-100 characters).";
    if (!blank(email) && (email.length() > 120 || !EMAIL.matcher(email.trim()).matches())) return "Please enter a valid email address.";
    if (!blank(contact) && !PHONE.matcher(contact.trim()).matches()) return "Please enter a valid contact number.";
    return null;
  }
  public static String password(String p) {
    if (p == null || p.length() < 8 || p.length() > 72) return "Password must be 8-72 characters.";
    if (!p.matches(".*[A-Za-z].*") || !p.matches(".*\\d.*")) return "Password must contain at least one letter and one number.";
    return null;
  }
  public static String account(String name, String username, String password, String email, String contact) {
    String e = profile(name, email, contact); if (e != null) return e;
    if (username == null || !USER.matcher(username.trim()).matches()) return "Username must be 4-30 characters: letters, numbers, dot, dash or underscore.";
    return password(password);
  }
  public static String child(Child c, boolean edit) {
    if (blank(c.getFullName()) || c.getFullName().trim().length() < 2 || c.getFullName().length() > 100) return "Please enter the child's full name.";
    if (!"Male".equals(c.getSex()) && !"Female".equals(c.getSex())) return "Please choose the child's sex.";
    if (blank(c.getMotherName()) || c.getMotherName().length() > 100) return "Please enter the mother's name.";
    if (c.getFatherName() != null && c.getFatherName().length() > 100) return "Father's name is too long.";
    if (c.getPurok() != null && c.getPurok().length() > 50) return "Purok is too long (50 characters maximum).";
    if (c.getPlaceOfDelivery() != null && c.getPlaceOfDelivery().length() > 100) return "Place of delivery is too long.";
    if (c.getBirthWeight() != null && (c.getBirthWeight() < 0.3 || c.getBirthWeight() > 7)) return "Birth weight must be between 0.3 and 7 kg.";
    if (!edit) {
      LocalDate b = c.getBirthDate();
      if (b == null || b.isAfter(LocalDate.now())) return "Please enter a valid birth date (not in the future).";
      if (b.isBefore(LocalDate.now().minusYears(10))) return "This system is for children up to 10 years old.";
    }
    return null;
  }
  public static String measure(Double height, Double weight) {
    if (height == null || height < 20 || height > 200) return "Height must be between 20 and 200 cm.";
    if (weight == null || weight < 0.5 || weight > 150) return "Weight must be between 0.5 and 150 kg.";
    return null;
  }
}
