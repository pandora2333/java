package pers.pandora.annotation;

import pers.pandora.constant.JSP;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
    String value() default JSP.NO_CHAR;
}
