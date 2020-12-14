package pers.pandora.annotation;

import pers.pandora.Enum.Propagation;
import pers.pandora.constant.LOG;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {

    int isolation() default Connection.TRANSACTION_REPEATABLE_READ;

    String[] no_rollback_exception() default {};

    Propagation propagation() default Propagation.REQUIRED;
}