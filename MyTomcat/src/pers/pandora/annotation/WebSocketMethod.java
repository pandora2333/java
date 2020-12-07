package pers.pandora.annotation;

import pers.pandora.constant.LOG;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketMethod {
    String value() default LOG.NO_CHAR;
}
