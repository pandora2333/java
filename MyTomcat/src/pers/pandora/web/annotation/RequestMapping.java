package pers.pandora.web.annotation;
import pers.pandora.common.constant.LOG;
import pers.pandora.web.constant.HTTPStatus;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default LOG.NO_CHAR;
    String method() default HTTPStatus.GET;
}
