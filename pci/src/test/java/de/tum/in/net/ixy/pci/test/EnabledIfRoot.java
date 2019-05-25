package de.tum.in.net.ixy.pci.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/** Enables a JUnit test if the user executing it is {@code root}. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfRootCondition.class)
@interface EnabledIfRoot { }
