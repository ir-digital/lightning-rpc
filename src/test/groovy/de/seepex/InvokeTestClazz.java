package de.seepex;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class InvokeTestClazz {

    private String key;
    private String value;

    public InvokeTestClazz() {

    }

    public InvokeTestClazz(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void iAmVoid() {
        
    }

    public boolean iReturnPrimitiveBoolean() {
        return true;
    }

    public Boolean iReturnBooleanObject() {
        return true;
    }

    public String iReturnAString() {
        return "hello";
    }

    public List<String> iReturnAListString() {
        return Collections.singletonList( "hello");
    }

    public List<HashMap<String, String>> iReturnAListOfHashMap() {
        return Collections.singletonList(new HashMap<>());
    }

    public HashMap<String, Integer> iReturnAHashMap() { return new HashMap<>(); }

    public Page<String> iReturnAPage() {
        return new PageImpl<>(new ArrayList<>());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
