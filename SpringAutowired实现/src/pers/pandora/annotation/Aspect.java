package pers.pandora.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    //The smaller the number, the higher the priority
    int value() default 1;
}
