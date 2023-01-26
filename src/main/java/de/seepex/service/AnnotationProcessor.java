package de.seepex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.seepex.annotation.ProviderType;
import de.seepex.annotation.SpxRpcMappedClass;
import de.seepex.annotation.SpxService;
import de.seepex.annotation.SpxServiceCommunicationDoc;
import de.seepex.domain.MethodCache;
import de.seepex.domain.RegisteredCache;
import de.seepex.domain.ReturnValue;
import de.seepex.domain.SpxClass;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.random.EasyRandom;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.reflections.scanners.Scanners.TypesAnnotated;

@Configuration
public class AnnotationProcessor implements ApplicationContextAware {

    private final ServiceCollector serviceCollector;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EasyRandom generator = new EasyRandom();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AnnotationExtractor annotationExtractor;
    private final ClassMappingHolder classMappingHolder = ClassMappingHolder.getInstance();
    private final CacheContainer cacheContainer;

    public AnnotationProcessor(ServiceCollector serviceCollector, AnnotationExtractor annotationExtractor, CacheContainer cacheContainer) {
        this.serviceCollector = serviceCollector;
        this.annotationExtractor = annotationExtractor;
        this.cacheContainer = cacheContainer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String serviceName = AnnotationExtractor.getServiceName(applicationContext);
        String routingKey = AnnotationExtractor.getRoutingKey(serviceName);
        String basePackage = getBasePackageToScan(applicationContext);

        if(basePackage == null) {
            logger.error("Failed to retrieve base package of application. Make sure there is a @SpringBootApplication annotated starter present.");
            SpringApplication.exit(applicationContext, () -> 0);
        }
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        try {
            collectSpxServiceClasses(routingKey, basePackage);
        } catch (ClassNotFoundException e) {
            logger.error("Failed to initialize", e);
            System.exit(0);
        }

        collectMappedClasses(basePackage);
    }

    private String getBasePackageToScan(ApplicationContext applicationContext) {
        Map<String, Object> candidates = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        Class<?> aClass = candidates.isEmpty() ? null : candidates.values().toArray()[0].getClass();
        if(aClass == null) {
            return null;
        }

        String name = aClass.getName();
        String simpleName = aClass.getSimpleName();
        return name.replace(simpleName, "");
    }

    /**
     * Collects all classes with the SpxRpcMappedClass annotation
     * currently only de.seepex packages are scanned
     */
    private void collectMappedClasses(final String packagePath) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packagePath))
                .setScanners(new TypeAnnotationsScanner()));

        Set<Class<?>> annotatedClasses = reflections.get(TypesAnnotated.with(SpxRpcMappedClass.class).asClass());

        logger.info("Scanning package {} --> Found {} SpxRpcMappedClass mappings", packagePath, annotatedClasses.size());
        for (Class<?> annotatedClass : annotatedClasses) {
            logger.info("Adding mapped class {}", annotatedClass.getName());
            SpxRpcMappedClass classAnnotation = annotatedClass.getAnnotation(SpxRpcMappedClass.class);
            classMappingHolder.addMapping(classAnnotation.mappingFor(), annotatedClass);
        }
    }

    private void collectSpxServiceClasses(String routingKey, String packagePath) throws ClassNotFoundException {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packagePath))
                .setScanners(new TypeAnnotationsScanner()));

        Set<Class<?>> classes = reflections.get(TypesAnnotated.with(SpxService.class).asClass());
        ClassInfoList classesImplementing = new ClassGraph().whitelistPackages(packagePath).enableClassInfo().scan().getClassesImplementing(MethodCache.class.getName());

        for(Class clazz : classes) {
            collectSpxServiceClasses(clazz, routingKey, classesImplementing);
        }
    }

    private void collectSpxServiceClasses(Class<?> objClz, String routingKey, ClassInfoList cacheClasses) throws ClassNotFoundException {
        final SpxService classAnnotation = objClz.getAnnotation(SpxService.class);
        if(classAnnotation == null) {
            return;
        }

        // register Service to routingkey
        final SpxClass spxClass = new SpxClass(classAnnotation.id(), objClz.getSimpleName(), classAnnotation.description());
        serviceCollector.registerRoutingKey(spxClass, routingKey, classAnnotation.providerType());
        if(classAnnotation.providerType().equals(ProviderType.EXCLUSIVE)) {
            annotationExtractor.setHasExclusiveServices(true);
        }

        for (Method m : objClz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(SpxServiceCommunicationDoc.class)) {
                SpxServiceCommunicationDoc methodAnnotation = m.getAnnotation(SpxServiceCommunicationDoc.class);

                String exampleObject = "";
                try {
                    Object example = generator.nextObject(Class.forName(m.getReturnType().getName()));
                    exampleObject = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
                } catch (final org.jeasy.random.ObjectCreationException | ClassNotFoundException exx) {
                    logger.info("Service-Documentation Prepare : Not found for EasyRandom use: {}", exx.getMessage());
                } catch (final Exception e) {
                    logger.debug(e.getMessage(), e);
                }

                ReturnValue returnValue = new ReturnValue();
                returnValue.setType(m.getReturnType().getSimpleName());
                returnValue.setExampleJson(exampleObject);

                // if an RPC method uses a cache, it needs to be announced to other services
                RegisteredCache registeredCache = null;
                if(StringUtils.isNotEmpty(methodAnnotation.cacheName())) {

                    // find all cache classes (implementing MethodCache) and register caches, that are used in rpc.
                    for(ClassInfo classInfo : cacheClasses) {
                        // cache was found and assigned
                        if(registeredCache != null) {
                            break;
                        }
                        Class clazz = Class.forName(classInfo.getName());
                        Object[] cacheEnums = clazz.getEnumConstants();

                        if(cacheEnums == null) {
                            continue;
                        }

                        for(Object cacheEnum: cacheEnums) {
                            if(!cacheEnum.toString().equalsIgnoreCase(methodAnnotation.cacheName())) {
                                continue;
                            }

                            TypeReference typeReference = ((MethodCache) cacheEnum).getTypeReference();
                            Long expireTimeSeconds = ((MethodCache) cacheEnum).getExpireTimeSeconds();

                            registeredCache = new RegisteredCache(methodAnnotation.cacheName(), typeReference, expireTimeSeconds);
                            registeredCache.setService(spxClass.getId());
                            registeredCache.setMethod(methodAnnotation.methodName());

                            cacheContainer.register(cacheEnum.toString(), registeredCache);
                            break;
                        }
                    }
                }

                serviceCollector.addMethod(spxClass, methodAnnotation.methodName(), methodAnnotation.description(),
                        registeredCache, Arrays.asList(m.getParameters().clone()), returnValue);
            }
        }
    }
}