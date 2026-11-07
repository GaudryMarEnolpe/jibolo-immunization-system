package com.jibolo.immunization.controller;
import com.jibolo.immunization.service.BusinessException;
import jakarta.servlet.http.*; import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException; import org.springframework.ui.Model;
import org.springframework.web.ErrorResponse; import org.springframework.web.bind.MissingServletRequestParameterException; import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; import org.springframework.web.servlet.FlashMap; import org.springframework.web.servlet.support.RequestContextUtils;
import java.net.URI; import java.util.*;

/** One place that turns any failure into a friendly message instead of a crash page. Stack traces are only written to the server log. */
@ControllerAdvice @Slf4j
public class ErrorAdvice {
  /** Only ever redirect to a path on THIS site (never to an outside address taken from the Referer header). */
  static String safePath(String ref, String fallback) {
    try { if (ref == null) return fallback; URI u = URI.create(ref); String p = u.getRawPath(); if (p == null || p.isEmpty()) return fallback; return p + (u.getRawQuery() != null ? "?" + u.getRawQuery() : ""); }
    catch (Exception e) { return fallback; }
  }
  private String back(HttpServletRequest req, String msg) {
    FlashMap fm = RequestContextUtils.getOutputFlashMap(req); fm.put("error", msg);
    return "redirect:" + safePath(req.getHeader("Referer"), "/");
  }
  @ExceptionHandler(BusinessException.class) String business(BusinessException e, HttpServletRequest r) { return back(r, e.getMessage()); }
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class) String conflict(HttpServletRequest r) { return back(r, "Someone else just changed this record. Please review it and try again."); }
  @ExceptionHandler(DataIntegrityViolationException.class) String integrity(HttpServletRequest r) { return back(r, "That could not be saved because it conflicts with existing records."); }
  @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class}) String badInput(HttpServletRequest r) { return back(r, "Please check the information you entered and try again."); }
  @ExceptionHandler(AccessDeniedException.class) void denied(AccessDeniedException e) { throw e; }   // let Spring Security answer (403 / login)

  @ExceptionHandler(Exception.class)
  String generic(Exception e, HttpServletResponse res, Model m) {
    int code = e instanceof NoSuchElementException ? 404 : e instanceof ErrorResponse er ? er.getStatusCode().value() : 500;
    String ref = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    if (code >= 500) log.error("Unexpected error, reference {}", ref, e);
    res.setStatus(code); m.addAttribute("status", code); m.addAttribute("ref", code >= 500 ? ref : null);
    return "error";
  }
}
