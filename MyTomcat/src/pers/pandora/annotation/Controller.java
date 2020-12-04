package pers.pandora.annotation;

import pers.pandora.constant.JSP;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Controller {
    String value() default JSP.NO_CHAR;
}
