package de.seepex.domain;

public class SomeTestClass implements SomeTestInterface{

    private String foo;

    @Override
    public String getName() {
        return "foo-bar";
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }
}
