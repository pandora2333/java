package pers.pandora.annotation;

import pers.pandora.constant.LOG;
import pers.pandora.constant.SQL;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String value() default LOG.NO_CHAR;
}
