package pers.pandora.annotation;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.LOG;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default LOG.NO_CHAR;
    String method() default HTTPStatus.GET;
}
