package pers.pandora.web.annotation;


import pers.pandora.common.constant.LOG;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
    String value() default LOG.NO_CHAR;
}
