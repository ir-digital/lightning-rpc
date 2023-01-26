package de.seepex.service;

import java.util.HashMap;
import java.util.Map;

public class ClassMappingHolder {

    private static ClassMappingHolder instance = null;
    private Map<String, Class<?>> fullQualifiedClassMapping = new HashMap<>();
    private Map<String, Class<?>> simpleClassMapping = new HashMap<>();

    private ClassMappingHolder() {
        // hide default constructor
    }

    public static ClassMappingHolder getInstance() {
        if(instance == null) {
            instance = new ClassMappingHolder();
        }

        return instance;
    }

    /**
     * Adds a mapping from returned class name to a local (in-service) representation.
     * className is expected to be a full qualified name (like de.seepex.domain.SomeClass) but can also be reduced to SomeClass.
     * Maybe we need to be careful here, if there multiple classes with the same name but different package. But in those cases we
     * still can use the fully qualified name.
     * localClass is the in-service class that should be mapped to the rpc response
     *
     * @param className
     * @param localClass
     */
    public void addMapping(String className, Class<?> localClass) {
        if(className.contains(".")) {
            fullQualifiedClassMapping.put(className, localClass);
        } else {
            simpleClassMapping.put(className, localClass);
        }

    }

    public Class<?> getMappedClass(String className) {
        // full qualified name (de.util.Foo)
        if(className.contains(".")) {
            Class<?> mappedClass = fullQualifiedClassMapping.get(className);
            if(mappedClass != null) {
                return mappedClass;
            }
        }

        // a simple name without full qualification has been passed, so we scan for the fist occurence of the Class name
        // ignoring the packages
        if(!className.contains(".")) {
            Class<?> mappedClass = simpleClassMapping.get(className);
            if(mappedClass != null) {
                return mappedClass;
            }
        }

        // nothing found yet: check if className is a full qualified name and was registered as simple name
        // de.util.Foo -> registered as Foo
        if(className.contains(".")) {
            String[] split = className.split("\\.");
            String simpleClassName = split[split.length - 1];

            Class<?> mappedClass = simpleClassMapping.get(simpleClassName);
            if(mappedClass != null) {
                return mappedClass;
            }
        }

        return null;
    }
}
