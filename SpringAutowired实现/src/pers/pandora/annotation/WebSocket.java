package pers.pandora.annotation;

import pers.pandora.constant.LOG;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocket {
    String value() default LOG.NO_CHAR;
}
