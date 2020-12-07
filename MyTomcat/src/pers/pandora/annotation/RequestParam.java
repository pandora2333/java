package pers.pandora.annotation;

import pers.pandora.constant.LOG;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value() default  LOG.NO_CHAR;
    String defaultValue() default LOG.NO_CHAR;
}
