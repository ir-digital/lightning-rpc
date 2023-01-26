package de.seepex.annotation;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpxServiceCommunicationDoc {

    String methodName();
    String description();
    String cacheKey() default StringUtils.EMPTY;
    String cacheName() default StringUtils.EMPTY;
}
