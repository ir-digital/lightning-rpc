package de.seepex

import de.seepex.domain.MetaReturn
import de.seepex.service.BasicJsonRpcProvider
import de.seepex.util.InvokePropertiesExtractor
import org.springframework.data.domain.Page
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Method

class InvokePropertiesExtractorSpec extends Specification {

    InvokeTestClazz invokeTestClazz = new InvokeTestClazz()



    @Unroll
    def "should recognize return object from return type string"() {

        when:
        def clazz = InvokePropertiesExtractor.getReturnClass(returnTypeName)

        then:
        noExceptionThrown()
        clazz.getReturnedBaseClass() == returnClass

        where:
        returnTypeName                                                          | returnClass
        "void"                                                                  | Void.class
        "java.lang.String"                                                      | String.class
        "java.util.List<java.lang.String>"                                      | List.class
        "java.util.List<java.util.HashMap<java.lang.String, java.lang.String>>" | List.class
        "boolean"                                                               | Boolean.class
        "java.lang.Boolean"                                                     | Boolean.class
        "java.util.HashMap<java.lang.String, java.lang.Integer>"                | HashMap.class
        "org.springframework.data.domain.Page<java.lang.String>"                | Page.class
    }

    @Unroll
    def "should recognize inner return object from return type string"() {

        when:
        MetaReturn clazz = InvokePropertiesExtractor.getReturnClass(returnTypeName)

        then:
        if(generics == null) {
            !clazz.getGenerics()
        } else {
            int foundCount = 0
            for(Class c : clazz.getGenerics()) {
                for(Class gc : generics) {
                    if(c.getName().equalsIgnoreCase(gc.getName())) {
                        foundCount += 1
                        break
                    }
                }
            }

            assert foundCount == generics.size()
        }

        where:
        returnTypeName                                                          | generics
        "void"                                                                  | null
        "java.lang.String"                                                      | null
        "java.util.List<java.lang.String>"                                      | [String.class]
        "java.util.List<java.util.HashMap<java.lang.String, java.lang.String>>" | [HashMap.class]
        "boolean"                                                               | null
        "java.lang.Boolean"                                                     | null
        "java.util.HashMap<java.lang.String, java.lang.Integer>"                | [String.class, Integer.class]
        "org.springframework.data.domain.Page<java.lang.String>"                | [String.class]
        "java.util.HashMap<java.lang.String, java.util.List<java.lang.String>>" | [String.class, List.class]
    }

    @Unroll
    def "should recognize non-container return types"() {
        given:
        Method method = getMethodByName(methodName)

        when:
        def result = InvokePropertiesExtractor.getReturnType(method)

        then:
        result == expectedResult

        where:
        methodName                | expectedResult
        "iAmVoid"                 | "void"
        "iReturnAString"          | "java.lang.String"
        "iReturnAListString"      | "java.util.List<java.lang.String>"
        "iReturnAListOfHashMap"   | "java.util.List<java.util.HashMap<java.lang.String, java.lang.String>>"
        "iReturnPrimitiveBoolean" | "boolean"
        "iReturnBooleanObject"    | "java.lang.Boolean"
        "iReturnAHashMap"         | "java.util.HashMap<java.lang.String, java.lang.Integer>"
        "iReturnAPage"            | "org.springframework.data.domain.Page<java.lang.String>"
    }

    @Unroll
    def "should recognized content of container"() {
        given:
        Method method = getMethodByName(methodName)

        when:
        def result = InvokePropertiesExtractor.containerContent(method)

        then:
        result == expectedResult

        where:
        methodName           | expectedResult
        "iAmVoid"            | ""
        "iReturnAString"     | ""
        "iReturnAListString" | "java.lang.String"
        "iReturnAHashMap"    | "java.lang.String, java.lang.Integer"
    }

    @Unroll
    def "should recognize list by clazz"() {
        when:
        def result = InvokePropertiesExtractor.isList(returnTypeClazz)

        then:
        result == expectedResult

        where:
        returnTypeClazz | expectedResult
        List.class      | true
        Page.class      | false
        String.class    | false
        Void.class      | false
    }

    @Unroll
    def "should recognize page by name"() {
        when:
        def result = InvokePropertiesExtractor.isPage(returnTypeClazz)

        then:
        result == expectedResult

        where:
        returnTypeClazz | expectedResult
        List.class      | false
        Page.class      | true
        String.class    | false
        Void.class      | false
    }

    @Unroll
    def "should recognize elements by name"() {
        when:
        def result = InvokePropertiesExtractor.isElement(returnTypeClazz)

        then:
        result == expectedResult

        where:
        returnTypeClazz | expectedResult
        List.class      | false
        Page.class      | false
        String.class    | true
        Void.class      | false
        Set.class       | false
    }

    @Unroll
    def "should recognize void by name"() {
        when:
        def result = InvokePropertiesExtractor.isVoid(returnTypeClazz)

        then:
        result == expectedResult

        where:
        returnTypeClazz | expectedResult
        List.class      | false
        Page.class      | false
        String.class    | false
        Void.class      | true
    }

    @Unroll
    def "should recognize containerContent type by name"() {
        when:
        def result = InvokePropertiesExtractor.containerContent(returnTypeName)

        then:
        result == expectedResult

        where:
        returnTypeName                                                    | expectedResult
        "List<de.seepex.domain.Location>"                                 | "de.seepex.domain.Location"
        "org.springframework.data.domain.Page<de.seepex.domain.Location>" | "de.seepex.domain.Location"
        "de.seepex.domain.Location"                                       | ""
        "void"                                                            | ""
        "HashMap<de.seepex.domain.Device, java.lang.Integer>"             | "de.seepex.domain.Device, java.lang.Integer"
    }

    Method getMethodByName(String name) {
        for (Method m : invokeTestClazz.getClass().getDeclaredMethods()) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m
            }
        }

        return null
    }
}
