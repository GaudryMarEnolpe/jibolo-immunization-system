package com.jibolo.immunization.service;
/** A rule was broken (bad date, duplicate, already done...). The message is safe to show to the user. */
public class BusinessException extends RuntimeException { public BusinessException(String m) { super(m); } }
