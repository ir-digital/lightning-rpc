package de.seepex.annotation;

import de.seepex.service.JsonRpcService;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({JsonRpcService.class})
@Documented
/**
 * This enables inter-service communication via rabbit
 */
public @interface EnableSpxRpc {

    String service();
}

