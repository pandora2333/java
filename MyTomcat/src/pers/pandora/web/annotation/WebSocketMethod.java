package pers.pandora.web.annotation;

import pers.pandora.common.constant.LOG;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketMethod {
    String value() default LOG.NO_CHAR;
}
