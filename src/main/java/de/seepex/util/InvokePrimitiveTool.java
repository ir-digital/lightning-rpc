package de.seepex.util;

import java.util.HashMap;
import java.util.Map;

public class InvokePrimitiveTool {

    private final static Map<String,Class> primitiveMap = new HashMap<>();

    static {
        primitiveMap.put("int", Integer.class );
        primitiveMap.put("long", Long.class );
        primitiveMap.put("double", Double.class );
        primitiveMap.put("float", Float.class );
        primitiveMap.put("boolean", Boolean.class );
        primitiveMap.put("char", Character.class );
        primitiveMap.put("byte", Byte.class );
        primitiveMap.put("void", Void.class );
        primitiveMap.put("short", Short.class );
    }

    public static String resolvePrimitives(String returnTypeName) {
        if(primitiveMap.containsKey(returnTypeName)) {
            return primitiveMap.get(returnTypeName).getName();
        }

        return returnTypeName;
    }

}
