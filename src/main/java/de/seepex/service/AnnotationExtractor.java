package de.seepex.service;


import de.seepex.annotation.EnableSpxRpc;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnnotationExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);

    private boolean hasExclusiveServices = false;

    public boolean hasExclusiveServices() {
        return hasExclusiveServices;
    }

    public void setHasExclusiveServices(boolean hasExclusiveServices) {
        this.hasExclusiveServices = hasExclusiveServices;
    }

    public static String getServiceName(ApplicationContext applicationContext) {
        final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(EnableSpxRpc.class);
        if (!beansWithAnnotation.isEmpty()) {
            final Object application = beansWithAnnotation.values().iterator().next();
            if (beansWithAnnotation.size() > 1) {
                String collected = beansWithAnnotation.values().stream().map(Object::toString).collect(Collectors.joining(", "));
                logger.warn("There are more than one Class in use with annotation @EnableSpxRpc. Taking the first one. -> {}\n the other ones -> {}", application.getClass(), collected);
            }

            final EnableSpxRpc annotation = AnnotationUtils.findAnnotation(application.getClass(), EnableSpxRpc.class);

            if(annotation != null) {
                return annotation.service();
            }
        }

        return null;
    }

    public static String getRoutingKey(String serviceName) {
        if(StringUtils.isEmpty(serviceName)) {
            return null;
        }
        return serviceName + "-json-cmd";
    }

    public static String getQueueName(String serviceName) {
        return "rpc-" + serviceName + "-json-cmd";
    }
}
