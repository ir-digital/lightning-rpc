package de.seepex.util;

import java.util.Set;

public class ContextAwareUtil {

    private ContextAwareUtil() {
        // secure
    }

    private static final ThreadLocal<Set> CONTEXT = new ThreadLocal<>();

    public static void setObject(Set remember) {
        CONTEXT.set(remember);
    }

    public static Set getObject() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
