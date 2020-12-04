package pers.pandora.annotation;

import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.JSP;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequestMapping {
    String value() default JSP.NO_CHAR;
    String method() default HTTPStatus.GET;
}
