package de.seepex.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import de.seepex.domain.MetaReturn;
import de.seepex.domain.PageImplBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class InvokeResultConstructor {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper mapper = new ObjectMapper();

    public InvokeResultConstructor() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String cleanUpIncomingPayload(String responsePayload) {
        // It comes Json in Json.
        String substring = responsePayload.substring(1);
        substring = substring.substring(0, substring.length() - 1).trim();
        substring = substring.replace("\\\"", "\"");
        substring = substring.substring(substring.indexOf(":") + 1).trim();
        substring = substring.substring(1);
        substring = substring.substring(0, substring.length() - 1).trim();
        return substring;
    }

    public LinkedHashMap getMap(MetaReturn metaReturn, String payload, Boolean cleanup) {
        try {
            String substring = payload;
            if(cleanup) {
                substring = cleanUpIncomingPayload(payload);
            }
            final LinkedHashMap map = new LinkedHashMap();
            if(substring.isEmpty()) {
                return map;
            }

            // Split each KeyValue entry
            final String[] split = substring.split(MapSerializer.SEPARATOR);
            for (int entryCount = 0; entryCount < split.length; entryCount++) {
                String entry = split[entryCount];
                // Got one Key-Value Pair
                final String[] keyValue = entry.split(MapSerializer.KVS);

                // Resolve Key
                final Class keyClass = metaReturn.getGenerics().get(0);
                final String keyJson = keyValue[0];
                final String realKeyJson = new String(Base64.getDecoder().decode(keyJson));
                final Object key = mapper.readValue(realKeyJson, keyClass);

                // Resolve Value
                final Class valueTarget = metaReturn.getGenerics().get(1);
                final String valueJson = keyValue[1];
                final String realValueJson = new String(Base64.getDecoder().decode(valueJson));
                final Object value = mapper.readValue(realValueJson, valueTarget);
                if(value == null) {
                    LOG.error("Receive Map containing null values for key {} over RPC payload-base64 -> {}", key, payload);
                    continue;
                }
                map.put(key, value);
            }

            return map;
        } catch (Exception e) {
            LOG.error("Failed to convert", e);
            return null;
        }
    }

    public <T> List<T> getList(String containedClass, String payload, List<String> typeHints) {
        try {
            // if there are typehints we need to map each object on its own
            if(!CollectionUtils.isEmpty(typeHints)) {
                HashMap<String, Class> typeHintedObjects = new HashMap<>();
                List<LinkedHashMap> elements = mapper.readValue(payload, List.class);

                List<T> result = new ArrayList<>();

                for(int i = 0; i < elements.size(); i++) {
                    LinkedHashMap element = elements.get(i);
                    String typeHintName = typeHints.get(i);
                    Class typeHint;
                    if(typeHintedObjects.containsKey(typeHintName)) {
                        typeHint = typeHintedObjects.get(typeHintName);
                    } else {
                        typeHint = Class.forName(typeHintName);
                        typeHintedObjects.put(typeHintName, typeHint);
                    }

                    Object mappedObject = mapper.convertValue(element, typeHint);
                    result.add((T) mappedObject);
                }

                return result;
            }
            // all objects of same type
            else {
                Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + InvokePrimitiveTool.resolvePrimitives(containedClass) + ";");
                T[] objects = mapper.readValue(payload, arrayClass);
                return Arrays.asList(objects);
            }
        } catch (Exception e) {
            LOG.error("Failed to convert", e);
            return null;
        }
    }

    public <T> Set<T> getSet(String containedClass, String payload) {
        try {
            Class<T[]> arrayClass = (Class<T[]>) Class.forName("[L" + InvokePrimitiveTool.resolvePrimitives(containedClass) + ";");
            T[] objects = mapper.readValue(payload, arrayClass);
            return Sets.newHashSet(objects);
        } catch (Exception e) {
            LOG.error("Failed to convert", e);
            return null;
        }
    }

    public <T> T getElement(Class clazz, String payload) {
        try {
            Object o = mapper.readValue(payload, clazz);
            return (T) o;
        } catch (Exception e) {
            LOG.error("Failed to convert", e);
            return null;
        }
    }

    public <T> Page<T> getPage(String containedClass, String payload) {
        try {
            containedClass = InvokePrimitiveTool.resolvePrimitives(containedClass);
            Class targetClass = Class.forName(containedClass);

            JavaType type = mapper.getTypeFactory().constructType(PageImplBean.class, targetClass);

            // this stunt is required to correctly map the pageable object in case if there are
            // no results. it will contain "INSTANCE" as string and mapping will fail otherwise
            HashMap payloadHash = mapper.readValue(payload, HashMap.class);
            if(payloadHash.containsKey("pageable") && payloadHash.get("pageable") instanceof String) {
                payloadHash.remove("pageable");
                payload = new Gson().toJson(payloadHash);
            }

            PageImplBean<T> bean = mapper.readValue(payload, type);

            // extract content of page in order to map it properly --> fix me i suck
            String s = mapper.writeValueAsString(bean.getContent());
            List<T> content = getList(containedClass, s, null);

            if (content == null) {
                content = new ArrayList<>();
            }

            for(T obj : content) {
                convertNestedMaps(obj);
            }

            bean.setContent(content);

            return bean.pageImpl();
        } catch (Exception e) {
            LOG.error("Failed to convert", e);
            return null;
        }
    }

    private void convertNestedMaps(Object object) {
        for (Field field : object.getClass().getDeclaredFields()) {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType)type;

                if(pType.getRawType().equals(Map.class)) {

                    try {
                        Field declaredField = object.getClass().getDeclaredField(field.getName());
                        declaredField.setAccessible(true);
                        Map innerMap = (Map) declaredField.get(object);

                        // no need to convert null or empty maps
                        if(innerMap == null || innerMap.keySet().isEmpty()) {
                            return;
                        }

                        Type actualTypeArgument = pType.getActualTypeArguments()[0];
                        Class<?> clazz1 = com.google.common.reflect.TypeToken.of(actualTypeArgument).getRawType();

                        actualTypeArgument = pType.getActualTypeArguments()[1];
                        Class<?> clazz2 = com.google.common.reflect.TypeToken.of(actualTypeArgument).getRawType();

                        // TODO: evaluate if this also works for maps like Map<String, List<String, String>>
                        MetaReturn metaReturn = new MetaReturn(LinkedHashMap.class, Arrays.asList(clazz1, clazz2));

                        String key = (String)innerMap.keySet().iterator().next();
                        String encodedMapContent = (String)innerMap.get(key);

                        Map map = getMap(metaReturn, encodedMapContent, false);
                        declaredField.set(object, map);
                    } catch (Exception e) {
                        LOG.error("Mapping failed", e);
                    }
                }

            }

        }
    }
}
