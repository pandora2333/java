package pers.pandora.annotation;

import pers.pandora.constant.LOG;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocket {
    String value() default LOG.NO_CHAR;
}
