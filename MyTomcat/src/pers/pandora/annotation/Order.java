package pers.pandora.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Order {
    int value() default 1;//数字越大，越先执行
}
