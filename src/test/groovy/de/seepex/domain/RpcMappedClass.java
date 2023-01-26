package de.seepex.domain;

import de.seepex.annotation.SpxRpcMappedClass;

@SpxRpcMappedClass(mappingFor = "SomeClassWeDontKnow")
public class RpcMappedClass {

    private String foo;

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }
}
