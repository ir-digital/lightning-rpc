package de.seepex.domain;

import java.util.List;

public class MetaReturn {

    private Class returnedBaseClass;
    private List<Class> generics;

    public Class getReturnedBaseClass() {
        return returnedBaseClass;
    }

    public List<Class> getGenerics() {
        return generics;
    }

    public MetaReturn(Class returnedBaseClass, List<Class> generics) {
        this.returnedBaseClass = returnedBaseClass;
        this.generics = generics;
    }

    @Override
    public String toString() {
        return "MetaReturn{" +
                "returnedBaseClass=" + returnedBaseClass +
                ", generics=" + generics +
                '}';
    }
}
