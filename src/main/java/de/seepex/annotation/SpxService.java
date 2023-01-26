package de.seepex.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface SpxService {

    String id();
    String description();
    ProviderType providerType() default ProviderType.SHARED;
    
}
