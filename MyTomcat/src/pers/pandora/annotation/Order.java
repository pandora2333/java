package pers.pandora.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Order {
    //more number can get more priority
    int value() default 1;
}
