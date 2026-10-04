package com.steelstorm.compat.neo.fml.common;

import com.steelstorm.compat.neo.api.distmarker.Dist;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid() default "";

    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};
}
