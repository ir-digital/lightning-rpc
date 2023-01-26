package de.seepex.domain;

public class SomeOtherTestClass implements SomeTestInterface{

    private String bar;

    @Override
    public String getName() {
        return "foo-bar-2";
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }
}
