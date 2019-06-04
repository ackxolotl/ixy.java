package de.tum.in.net.ixy.memory.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation that disallows the execution of a test case if the member {@link
 * de.tum.in.net.ixy.memory.BuildConfig#OPTIMIZED} is {@code true}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledIfOptimizedCondition.class)
@interface DisabledIfOptimized { }
