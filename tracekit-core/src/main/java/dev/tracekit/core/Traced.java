package dev.tracekit.core;

import java.lang.annotation.*;

/** Marks a Spring-managed bean method (or all public methods of a bean class) for tracing. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traced {
    /** Record argument values. */
    boolean args() default true;
    /** Record the return value. */
    boolean result() default true;
}
