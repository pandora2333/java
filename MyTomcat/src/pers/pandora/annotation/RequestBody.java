package pers.pandora.annotation;

import pers.pandora.constant.LOG;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
    String value() default LOG.NO_CHAR;
}
