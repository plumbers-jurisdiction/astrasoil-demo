package com.henribdev.nasa.challenge.astrasoil.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Profile;

// Marks a bean as demo-only: it's only created when the demo profile is active (application-demo.yaml).
// The demo profile is the default; production sets its own profile with SPRING_PROFILES_ACTIVE.
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Profile(ConditionalOnDemo.PROFILE)
public @interface ConditionalOnDemo {

    String PROFILE = "demo";

}
