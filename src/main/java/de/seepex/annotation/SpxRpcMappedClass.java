package de.seepex.annotation;

/**
 *
 * This annotation can be used to utilize soft-contracts between services.
 * Example:
 * service A returns a Class de.util.Foo in a SpxRPC response.
 * service B calls this class, but doesnt know de.util.Foo.
 *
 * In service B we create a mapping class de.util.Foo2 (or whatever name we want to use) and annotate it with
 * @SpxRpcMappedClass(mappingFor = de.util.Foo)
 * or even
 * @SpxRpcMappedClass(mappingFor = Foo)
 *
 * the RPC infrastructure will then know how to map the response and will return Foo2 as result of the rpc call
 */

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpxRpcMappedClass {

    String mappingFor();
}

