package de.seepex.util;


import de.seepex.domain.MetaReturn;
import de.seepex.service.ClassMappingHolder;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class InvokePropertiesExtractor {

    private final static Logger LOG = LoggerFactory.getLogger(InvokePropertiesExtractor.class);

    private static PassiveExpiringMap<String, MetaReturn> metaReturnCache = new PassiveExpiringMap(new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(1, TimeUnit.HOURS), new HashMap<>());

    public static String getReturnType(Method method) {
        return method.getGenericReturnType().getTypeName();
    }

    public static String containerContent(Method method) {
        String typeName = method.getGenericReturnType().getTypeName();
        if(!typeName.contains("<")) {
            return StringUtils.EMPTY;
        }

        String inside = typeName.substring(typeName.indexOf("<") + 1, typeName.length() - 1);

        // for now we strip inner types. so List<HashMap<String, String>> will return HashMap as containerContent
        int subTypingIndex = inside.indexOf("<");
        if(subTypingIndex > -1) {
            return inside.substring(0, subTypingIndex);
        }

        return inside;
    }

    public static Class getReturnClass(Method method) {
        String returnType = getReturnType(method);

        // strip "<" in case its a parametrized type
        if(returnType.contains("<")) {
            returnType = returnType.substring(0, returnType.indexOf("<"));
        }

        return getClassFor(returnType);
    }

    public static MetaReturn getReturnClass(String returnTypeName) {
        MetaReturn metaReturnFromCache = metaReturnCache.getOrDefault(returnTypeName, null);
        if(metaReturnFromCache != null) {
            return metaReturnFromCache;
        }

        // when there is no info on the returned class --> convert to string
        if(StringUtils.isEmpty(returnTypeName)) {
            return new MetaReturn(String.class, new ArrayList<>());
        }

        String className = returnTypeName;
        final List<Class> generics = new ArrayList<>();
        if(returnTypeName.contains("<")) {
            className = returnTypeName.substring(0, returnTypeName.indexOf("<"));

            String innerTypes = returnTypeName.substring(returnTypeName.indexOf("<") + 1, returnTypeName.lastIndexOf(">"));
            String[] allGenerics = innerTypes.split(",");

            for (String aGeneric : allGenerics) {

                // this is when inner generic contains objects -> for example HashMap<String, String>>
                // in this case we only need the outer type -> HashMap
                if(aGeneric.contains("<")) {
                    aGeneric = aGeneric.substring(0, aGeneric.indexOf("<"));
                    aGeneric = aGeneric.split(",")[0];
                }

                if(aGeneric.contains(">")) {
                    continue;
                }

                Class clazz = getClassFor(aGeneric.trim());
                generics.add(clazz);
            }
        }

        MetaReturn metaReturn = new MetaReturn(getClassFor(className), generics);
        metaReturnCache.put(returnTypeName, metaReturn);

        return metaReturn;
    }

    private static Class getClassFor(String className) {
        Class<?> mappedClass = ClassMappingHolder.getInstance().getMappedClass(className);
        if(mappedClass != null) {
            return mappedClass;
        }

        try {
            String resolvedPrimitives = InvokePrimitiveTool.resolvePrimitives(className);
            return Class.forName(resolvedPrimitives);
        } catch (ClassNotFoundException e) {
            LOG.debug("Failed to convert: " + className, e);
            return null;
        }
    }

    public static boolean isMap(Class returnClazz) {
        return Map.class.isAssignableFrom(returnClazz);
    }

    public static boolean isList(Class returnClazz) {
        return List.class.isAssignableFrom(returnClazz);
    }

    public static boolean isSet(Class returnClazz) {
        return Set.class.isAssignableFrom(returnClazz);
    }

    public static boolean isPage(Class returnClazz) {
        return Page.class.isAssignableFrom(returnClazz);
    }

    public static String containerContent(String returnTypeName) {
        if(!returnTypeName.contains("<")) {
            return StringUtils.EMPTY;
        }

        String inside = returnTypeName.substring(returnTypeName.indexOf("<") + 1, returnTypeName.length() - 1);

        // for now we strip inner types. so List<HashMap<String, String>> will return HashMap as containerContent
        int subTypingIndex = inside.indexOf("<");
        if(subTypingIndex > -1) {
            return inside.substring(0, subTypingIndex);
        }

        return inside;
    }

    public static boolean isVoid(Class returnClazz) {
        return Void.class.isAssignableFrom(returnClazz);
    }

    public static boolean isElement(Class returnClazz) {
        return !isVoid(returnClazz) && !isPage(returnClazz) && !isList(returnClazz) && !isMap(returnClazz) && !isSet(returnClazz);
    }

}
