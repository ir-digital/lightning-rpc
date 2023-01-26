package de.seepex.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import de.seepex.annotation.SpxService;
import de.seepex.annotation.SpxServiceCommunicationDoc;
import de.seepex.domain.*;
import de.seepex.infrastructure.exception.NotSupportedException;
import de.seepex.util.ContextAwareUtil;
import de.seepex.util.InvokePropertiesExtractor;
import de.seepex.util.RpcContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasicJsonRpcProvider {

    protected ApplicationContext applicationContext;
    protected ObjectMapper objectMapper = new ObjectMapper();

    public BasicJsonRpcProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private Object getClass(String id) {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object obj = applicationContext.getBean(beanName);

            Class<?> objClz = obj.getClass();
            if (org.springframework.aop.support.AopUtils.isAopProxy(obj)) {
                objClz = org.springframework.aop.support.AopUtils.getTargetClass(obj);
            }

            if (objClz.isAnnotationPresent(SpxService.class)) {
                SpxService classAnnotation = objClz.getAnnotation(SpxService.class);

                if (classAnnotation.id().equalsIgnoreCase(id)) {
                    return obj;
                }
            }
        }

        return null;
    }

    private boolean isInterfaceMethod(Class toplevel, Method method) {
        final Class[] interfaces = toplevel.getInterfaces();
        for (Class anInterface : interfaces) {
            try {
                final Method interfaceEquivalent = anInterface.getMethod(method.getName(), method.getParameterTypes());
                if (interfaceEquivalent.getReturnType().equals(method.getReturnType())) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                // Not interesting
            }
        }
        return false;
    }

    private Method getMethod(Class<?> clazz, String methodName) {
        final List<Method> collect = Arrays.stream(clazz.getDeclaredMethods()).sorted((m1, m2) ->
                isInterfaceMethod(clazz, m1) ? 1 : -1
        ).collect(Collectors.toList());

        for (Method m : collect) {
            if (m.isAnnotationPresent(SpxServiceCommunicationDoc.class)) {
                SpxServiceCommunicationDoc methodAnnotation = m.getAnnotation(SpxServiceCommunicationDoc.class);

                if (methodAnnotation.methodName().equalsIgnoreCase(methodName)) {
                    return m;
                }
            }
        }

        return null;
    }

    private Class get(Type actualTypeArgument) throws ClassNotFoundException {
        String typeName = actualTypeArgument.getTypeName();

        // that fixes nested objects in the list like List<HashMap<Object, Object>>
        if(typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }

        return Class.forName(typeName);
    }

    private List<Object> getArguments(Method method, RpcRequest request) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();

        List<Param> params = request.getParams();

        if (parameters.length > params.size()) {
            LOG.error("Argument missmatch. Expected {} arguments. Received {} arguments. Called method {}.{}", parameters.length, params.size(), request.getServiceId(), method.getName());
            return null;
        }

        ArrayList<Object> arguments = new ArrayList<>();

        for (int i = 0; i < parameterTypes.length; i++) {
            String parameterName = parameters[i].getName();

            Param processedParam = params.get(i);

            // if parameter is a primitive, we find a matching wrapper
            Class<?> parameterClass;
            if (parameterTypes[i].isPrimitive()) {
                parameterClass = ClassUtils.primitiveToWrapper(parameterTypes[i]);
            } else {
                parameterClass = Class.forName(parameterTypes[i].getCanonicalName());
            }

            final boolean hasProperParamName = processedParam.getName().equals(parameterName);

            if (!hasProperParamName) {
                // this means param name doesnt match the expected parameter name.
                // we will still try to envoke the method and assume that the parameters are sent in the right order
                // a warning log will be written

                final String msg = "Parameters for call " + request.getServiceId() + "." + method.getName() + " do not match. Expected " + parameterName +
                        ", but received " + processedParam.getName() + ". " +
                        "Will assume that the submitted parameters are in correct order and ignore the naming. Still should be fixed in the calling class!" +
                        ". RpcContext: [" + RpcContext.getAsString() + "]";
                LOG.warn(msg);
            }
            
            String paramJson = new Gson().toJson(processedParam.getValue());

            // we need to construct page object on ourselves, as it doesn't work through objectmapper
            if (parameterClass.getSimpleName().equalsIgnoreCase("Pageable")) {
                CustomPageable customPageable = objectMapper.readValue(paramJson, CustomPageable.class);

                List<Sort.Order> orders = new ArrayList<>();
                Sort sort = null;

                if (customPageable.getSort() != null) {

                    for (CustomSortOrder customSortOrder : customPageable.getSort().getSortOrders()) {
                        Sort.Direction direction = Sort.Direction.DESC;

                        if (StringUtils.isNotEmpty(customSortOrder.getDirection())) {
                            if (customSortOrder.getDirection().equals(Sort.Direction.ASC.name())) {
                                direction = Sort.Direction.ASC;
                            }
                        }

                        Sort.Order order = new Sort.Order(direction, customSortOrder.getProperty());
                        orders.add(order);
                    }

                    sort = Sort.by(orders);
                }

                PageRequest pageRequest;
                if (sort == null) {
                    pageRequest = PageRequest.of(customPageable.getPageNumber(), customPageable.getPageSize());
                } else {
                    pageRequest = PageRequest.of(customPageable.getPageNumber(), customPageable.getPageSize(), sort);
                }

                arguments.add(pageRequest);
            }

            // param is a set
            else if (parameterClass.getName().equalsIgnoreCase("java.util.Set")) {
                Type actualTypeArgument = ((ParameterizedType) method.getParameters()[i].getParameterizedType()).getActualTypeArguments()[0];
                Class typeClass = Class.forName(actualTypeArgument.getTypeName());

                JavaType type = objectMapper.getTypeFactory().constructCollectionType(Set.class, typeClass);
                Object paramObject = objectMapper.readValue(paramJson, type);
                arguments.add(paramObject);
            }

            // if param is a list
            else if (Collection.class.isAssignableFrom(parameterClass)) {
                Type actualTypeArgument = ((ParameterizedType) method.getParameters()[i].getParameterizedType()).getActualTypeArguments()[0];
                Class typeClass = get(actualTypeArgument);

                JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, typeClass);
                Object paramObject = objectMapper.readValue(paramJson, type);
                arguments.add(paramObject);
            }

            // if param is a map
            else if (Map.class.isAssignableFrom(parameterClass)) {
                // complex map like Map<UUID, Long>
                if( method.getParameters()[i].getParameterizedType() instanceof ParameterizedType ) {
                    Type argument1 = ((ParameterizedType) method.getParameters()[i].getParameterizedType()).getActualTypeArguments()[0];
                    Type argument2 = ((ParameterizedType) method.getParameters()[i].getParameterizedType()).getActualTypeArguments()[1];

                    Class typeClass1 = get(argument1);
                    Class typeClass2 = get(argument2);

                    JavaType type = objectMapper.getTypeFactory().constructMapLikeType(Map.class, typeClass1, typeClass2);
                    Object paramObject = objectMapper.readValue(paramJson, type);
                    arguments.add(paramObject);
                } else {
                    LOG.error("Generic maps are not supported through RPC calls! Please define types of map like Map<String, Integer>. Affected method {}", method.getName());
                    throw new NotSupportedException("Generic maps are not supported through RPC calls!");
                }
            }

            // param is a array
            else if (parameterClass.isArray()) {
                LOG.error("Array conversion not implemented yet!!!");
                throw new RuntimeException("Not implemented!");
            } else {
                Object paramObject;
                Class<?> targetClass = parameterClass;

                // caller wants to us cast to the submitted object type specifically, so we will use
                // the objectmapper encoded version, which has the necessary data for decoding into abstract classes
                if(Boolean.TRUE.equals(processedParam.getForceObjectType())) {
                    targetClass = Class.forName(processedParam.getObjectType());
                    paramObject = objectMapper.readValue(processedParam.getObjectMapperCoding(), targetClass);
                } else {
                    paramObject = objectMapper.readValue(paramJson, targetClass);
                }

                arguments.add(paramObject);
            }
        }

        return arguments;
    }

    /**
     * This method will return a list of implementing classes in the response in case the return type
     * is a interface. This is required on the receiving side, otherwise the RPC doesnt know, in which class
     * the response needs to be converted
     *
     * @param method
     * @return
     */
    private List<String> getReturnTypeHints(Method method, Object response) {
        List<String> typeHints = new ArrayList<>();
        if(response == null) {
            return typeHints;
        }

        // currently we only check for Lists<SomeContainedClass>
        if(InvokePropertiesExtractor.isList(method.getReturnType())) {
            String typeName = method.getGenericReturnType().getTypeName();
            String containerContent = InvokePropertiesExtractor.containerContent(typeName);

            if(StringUtils.isNotEmpty(containerContent)) {
                try {
                    Class<?> containedClass = Class.forName(containerContent);

                    // if we are here, it means that the return type is a interface, so actual implementation
                    // names must be hinted for the receiving side, in order to decode it correctly
                    if(containedClass.isInterface()) {
                        List<Object> castedReponse = (List<Object>) response;
                        for(Object responseEntry : castedReponse) {
                            String entryName = responseEntry.getClass().getName();
                            typeHints.add(entryName);
                        }

                        return typeHints;
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("Failed to instantiate contained class by name " + containerContent + ". Type hinting wont work", e);
                    return typeHints;
                }
            }
        }

        return typeHints;
    }

    private InvokeResult callMethod(RpcRequest request) throws Exception {
        InvokeResult invokeResult = new InvokeResult();
        Object c = getClass(request.getServiceId());

        if (c == null) {
            LOG.error("Failed to get class for serviceId {}", request.getServiceId());
            return null;
        }

        Class<?> clazz = c.getClass();
        if (org.springframework.aop.support.AopUtils.isAopProxy(c)) {
            clazz = org.springframework.aop.support.AopUtils.getTargetClass(c);
        }

        Method method = getMethod(clazz, request.getMethod());

        if (method == null) {
            LOG.error("Failed to get method with name {} on serviceId {}", request.getMethod(), request.getServiceId());
            return null;
        }

        List<Object> arguments = getArguments(method, request);

        method.setAccessible(true);
        try {
            Object invoke = method.invoke(c, arguments.toArray());
            List<String> returnTypeHints = getReturnTypeHints(method, invoke);

            invokeResult.setMethod(method);
            invokeResult.setResponse(invoke);
            invokeResult.setResponseTypeHints(returnTypeHints);

            return invokeResult;
        } catch (Exception e) {
            invokeResult.setFailed(true);

            if (e instanceof InvocationTargetException) {
                final InvocationTargetException ite = (InvocationTargetException) e;
                LOG.error("Method " + request.getServiceId() + "." + request.getMethod() + " threw an exception. " +
                        "It will be forwarded to the rpc caller. RcpContext: " + RpcContext.getAsString(), ite.getTargetException());

                invokeResult.setExceptionText(ite.getTargetException().getMessage());
                invokeResult.setExceptionClassName(ite.getTargetException().getClass().getName());
            } else {
                LOG.error("Failed to invoke method " + request.getServiceId() + "." + request.getMethod(), e);

                invokeResult.setExceptionText(e.getMessage());
                invokeResult.setExceptionClassName(e.getClass().getName());
            }

            return invokeResult;
        }
    }

    protected InvokeResult serve(RpcRequest request, HashMap<String, String> applicationHeaders) {
        try {
            RpcContext.setApplicationHeaders(applicationHeaders);
            return callMethod(request);
        } catch (Throwable e) {
            LOG.error("Failed to make call", e);
        } finally {
            RpcContext.clear();
            ContextAwareUtil.clear();
        }

        return null;
    }

}
