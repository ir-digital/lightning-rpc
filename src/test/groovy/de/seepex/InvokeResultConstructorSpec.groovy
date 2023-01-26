package de.seepex

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.gson.Gson
import de.seepex.domain.*
import de.seepex.util.InvokeResultConstructor
import de.seepex.util.MapSerializer
import org.springframework.data.domain.Page
import spock.lang.Specification

class InvokeResultConstructorSpec extends Specification {

    InvokeResultConstructor invokeResultConstructor = new InvokeResultConstructor()

    def "should construct Set"() {
        given:
        String className = "java.util.UUID"

        and:
        String payload = new Gson().toJson([UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()])

        when:
        Set<UUID> set = invokeResultConstructor.<UUID> getSet(className, payload)

        then:
        set.size() == 3
    }

    def "should construct list"() {
        given:
        String className = "java.lang.String"

        and:
        String payload = new Gson().toJson(["a", "b", "c"])

        when:
        List<String> list = invokeResultConstructor.<String> getList(className, payload, null)

        then:
        list.size() == 3
    }

    def "should construct list of primitive booleans"() {
        given:
        String className = "boolean"

        and:
        String payload = new Gson().toJson([true, false])

        when:
        List<Boolean> list = invokeResultConstructor.<Boolean> getList(className, payload, null)

        then:
        list.size() == 2
        list.get(0)
        !list.get(1)
    }

    def "should construct single class"() {
        given:
        String payload = new Gson().toJson("I am the content")

        when:
        def result = invokeResultConstructor.<String> getElement(String.class, payload)

        then:
        result == "I am the content"
    }

    def "should construct boolean class"() {
        given:
        String payload = new Gson().toJson(true)

        when:
        def result = invokeResultConstructor.<Boolean> getElement(Boolean.class, payload)

        then:
        result
    }

    def "should construct page container"() {
        given:
        String className = InvokeTestClazz.getName()

        and:
        PageImplBean page = new PageImplBean()
        page.setContent([new InvokeTestClazz("a1", "b1"), new InvokeTestClazz("a2", "b2")])
        String payload = new Gson().toJson(page)

        when:
        def result = invokeResultConstructor.<Page<InvokeTestClazz>> getPage(className, payload)

        then:
        result.getContent().size() == 2
    }

    def "should construct a hashmap"() {
        given:
        LinkedHashMap<Param, Integer> map = new LinkedHashMap<>()
        def paramA = new Param("A", "An A")
        map.put(paramA, 1)
        def paramB = new Param("B", "A B")
        map.put(paramB, 2)
        def paramC = new Param("C", "A C\tFOO")
        map.put(paramC, 3)

        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        String payload = objectMapper.writeValueAsString(map)

        and:
        MetaReturn metaReturn = new MetaReturn(HashMap.class, [Param.class, Integer.class])

        when:
        Map<Param, Integer> result = invokeResultConstructor.getMap(metaReturn, payload, true)

        then:
        result.size() == 3
        result.containsValue(1)
        result.containsValue(2)
        result.containsValue(3)
        Map.Entry<Param, Integer> entry = result.entrySet().asList().get(0)
        entry.getKey().name == "A"
        entry.getKey().value == "An A"
        entry.getValue() == 1
    }

    def "should contruct a nested map"() {
        given:
        DeviceForTest device = new DeviceForTest()
        device.setId(UUID.randomUUID())
        device.setConfig([name: "Jerry"])

        HashMap<DeviceForTest, Integer> map = new HashMap<>()
        map.put(device, 1)

        and:
        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        String payload = objectMapper.writeValueAsString(map)
        MetaReturn metaReturn = new MetaReturn(HashMap.class, [DeviceForTest.class, Integer.class])

        when:
        HashMap<DeviceForTest, Integer> result = invokeResultConstructor.getMap(metaReturn, payload, true)

        then:
        result.size() == 1

    }

    def "should construct a byte []"() {
        given:
        byte[] useless = new String("Useless strings").getBytes();

        and:
        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        String payload = objectMapper.writeValueAsString(useless)

        when:
        byte[] usefull = invokeResultConstructor.getElement(byte[].class, payload)

        then:
        usefull.length == usefull.length
        new String(usefull) == new String(useless)
    }

    def "should construct nested objects"() {
        given:
        HashMap<String, String> metaData = new HashMap<>();
        metaData.put("one", "string1")
        metaData.put("two", "string2")
        metaData.put("three", "string3")

        FeLocationSimple feLocationSimple = new FeLocationSimple()
        feLocationSimple.setDescription("simple desc")
        def feLocationSimpleId = UUID.randomUUID()
        feLocationSimple.setId(feLocationSimpleId)
        feLocationSimple.setName("simplename")
        feLocationSimple.setDeviceCount(1)

        FeDeviceSimple feDeviceSimple = new FeDeviceSimple()
        feDeviceSimple.setId(UUID.randomUUID())
        feDeviceSimple.setDescription("fedescr")
        feDeviceSimple.setCommNr("commnr")
        feDeviceSimple.setType("tpyxer")
        feDeviceSimple.setConfiguration(metaData)


        FeLocation feLocation = new FeLocation()
        feLocation.setName("locationname")
        feLocation.setId(UUID.randomUUID())
        feLocation.setDescription("loc description")
        feLocation.setDevices(Arrays.asList(feDeviceSimple))
        feLocation.setChildren(Arrays.asList(feLocationSimple))
        feLocation.setParent(feLocationSimple)
        feLocation.setSubLocationCount(1)

        FeDeviceSimple feDevice = new FeDeviceSimple()
        feDevice.setCommNr("foo")
        feDevice.setType("type")
        feDevice.setNotes("notes")

        FeSensor feSensor = new FeSensor()
        feSensor.setFieldId("aFieldId")
        def feSensorId = UUID.randomUUID()
        feSensor.setId(feSensorId)
        feSensor.setLocation("testrunner")
        feSensor.setMappedUnit("kg/mojo")
        feSensor.setMetadata(metaData)
        feSensor.setName("feSensorName")
        feSensor.setNotes("secret notes")
        feSensor.setType(UUID.randomUUID());
        feSensor.setTime(UUID.fromString("0048afc0-857a-11eb-8080-808080808080"))
        feSensor.setUnit("g/mini")
        feSensor.getMappedOrRawUnit()
        feSensor.addDevice(feDevice)

        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        String payload = objectMapper.writeValueAsString(feSensor)

        when:
        FeSensor result = invokeResultConstructor.getElement(FeSensor.class, payload)

        then:
        result instanceof FeSensor
        result.getId() == feSensorId
        feSensor.getDevices().get(0).getCommNr() == "foo"
    }

    def "should construct page of objects with a hashmap inside"() {
        given:
        HashMap<String, String> metaData = new HashMap<>();
        metaData.put("one", "string1")
        metaData.put("two", "string2")
        metaData.put("three", "string3")

        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        and:
        FeDeviceSimple device = new FeDeviceSimple()
        device.setCommNr("123")
        device.setConfiguration(metaData)

        and:
        PageImplBean pageImplBean = new PageImplBean()
        pageImplBean.setContent(Collections.singletonList(device))

        String payload = objectMapper.writeValueAsString(pageImplBean.pageImpl())

        when:
        Page<FeDeviceSimple> result = invokeResultConstructor.getPage(FeDeviceSimple.getName(), payload)

        then:
        result instanceof Page
        result.getContent().get(0).getCommNr() == "123"
        result.getContent().get(0).getConfiguration().containsKey("one")
        result.getContent().get(0).getConfiguration().containsKey("two")
        result.getContent().get(0).getConfiguration().containsKey("three")
    }

    def "should filter out null values from Map conversion"() {
        given:
        HashMap<String,String> metaData = new HashMap<>();
        def uuidString = UUID.randomUUID().toString()
        metaData.put("EXTERNAL_ID", uuidString);
        metaData.put("FOO", null);
        metaData.put("KEY", "VALUE");

        ObjectMapper objectMapper = new ObjectMapper()
        SimpleModule module = new SimpleModule()
        module.addSerializer(new MapSerializer())
        objectMapper.registerModule(module)

        String payload = objectMapper.writeValueAsString(metaData)

        and:
        MetaReturn metaReturn = new MetaReturn(HashMap.class, [String.class, String.class])

        when:
        Map<String, String> result = invokeResultConstructor.getMap(metaReturn, payload, true)

        then:
        result.size() == 2
        result.get("EXTERNAL_ID") == uuidString
        result.get("KEY") == "VALUE"
    }

    def "should decode a list with hints"() {
        given:
        SomeTestClass class1 = new SomeTestClass()
        class1.setFoo("foo1")

        SomeOtherTestClass class2 = new SomeOtherTestClass()
        class2.setBar("bar2")

        and:
        def json = new ObjectMapper().writeValueAsString([class1, class2])

        when:
        def result = invokeResultConstructor.getList(SomeTestInterface.class.getName(), json, [class1.getClass().getName(), class2.getClass().getName()])

        then:
        noExceptionThrown()
        result.get(0).getClass().getName() == "de.seepex.domain.SomeTestClass"
        result.get(1).getClass().getName() == "de.seepex.domain.SomeOtherTestClass"
    }
}
