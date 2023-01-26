package de.seepex;

import de.seepex.annotation.SpxService;
import de.seepex.annotation.SpxServiceCommunicationDoc;
import de.seepex.domain.SomeOtherTestClass;
import de.seepex.domain.SomeTestClass;
import de.seepex.domain.SomeTestInterface;

import java.util.*;

@SpxService(id = "foo-service", description = "")
public class TestClass extends DeviceForTest implements TestInterface {

    @Override
    @SpxServiceCommunicationDoc(methodName = "getMap", description = "fofofoo")
    public HashMap getMap() {
        return null;
    }

    @SpxServiceCommunicationDoc(methodName = "getList", description = "fofofoo")
    public ArrayList getList() {
        return null;
    }

    @SpxServiceCommunicationDoc(methodName = "someMethod", description = "fofofoo")
    public String someMethod(String firstParam, String secondParam) {
        return firstParam + secondParam;
    }

    @SpxServiceCommunicationDoc(methodName = "updateFeatures", description = "fofofoo")
    public String updateFeatures(UUID tenantId, List<Map<String, Object>> features) {
        return "im fine";
    }

    @SpxServiceCommunicationDoc(methodName = "returnStuff", description = "fofofoo")
    public List<SomeTestInterface> returnStuff(UUID tenantId) {
        SomeTestClass class1 = new SomeTestClass();
        class1.setFoo("foo1");

        SomeOtherTestClass class2 = new SomeOtherTestClass();
        class2.setBar("bar2");

        return Arrays.asList(class1, class2);
    }

    public String getString() {
        return null;
    }
}
