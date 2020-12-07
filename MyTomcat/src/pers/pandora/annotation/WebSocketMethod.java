package pers.pandora.annotation;

import pers.pandora.constant.JSP;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketMethod {
    String value() default JSP.NO_CHAR;
}
