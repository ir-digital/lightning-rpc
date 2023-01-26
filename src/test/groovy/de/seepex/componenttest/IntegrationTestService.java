package de.seepex.componenttest;

import com.google.gson.Gson;
import de.seepex.DeviceForTest;
import de.seepex.annotation.ProviderType;
import de.seepex.annotation.SpxService;
import de.seepex.annotation.SpxServiceCommunicationDoc;
import de.seepex.domain.ClassWithAMap;
import de.seepex.domain.SomeTestClass;
import de.seepex.domain.TestCache;
import de.seepex.service.BaseJsonRpcConnector;
import de.seepex.service.SpxCacheManager;
import de.seepex.util.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.*;

@Profile("integrationtest") 
@SpxService(id = "integrationtest-service", description = "", providerType = ProviderType.EXCLUSIVE)
public class IntegrationTestService {

    private BaseJsonRpcConnector baseJsonRpcConnector;
    private SpxCacheManager spxCacheManager;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public IntegrationTestService(BaseJsonRpcConnector baseJsonRpcConnector, SpxCacheManager cacheManager) {
        this.baseJsonRpcConnector = baseJsonRpcConnector;
        this.spxCacheManager = cacheManager;
    }

    @SpxServiceCommunicationDoc(methodName = "testMethod1", description = "")
    public String testMethod1() {
        return baseJsonRpcConnector.rpc("testMethod2", "integrationtest-service");
    }

    @SpxServiceCommunicationDoc(methodName = "testMethod2", description = "")
    public String testMethod2() {
        Map<String, String> applicationHeaders = RpcContext.getApplicationHeaders();
        return new Gson().toJson(applicationHeaders);
    }

    @SpxServiceCommunicationDoc(methodName = "exceptionThrowingMethod", description = "")
    public String exceptionThrowingMethod() throws Exception {
        throw new Exception("i am dead");
    }

    @SpxServiceCommunicationDoc(methodName = "stringMethod", description = "")
    public String stringMethod() {
        return "ok ok ok";
    }

    @SpxServiceCommunicationDoc(methodName = "fooMethod", description = "")
    public SomeTestClass fooMethod() {
        SomeTestClass someTestClass = new SomeTestClass();
        someTestClass.setFoo("bar");
        return someTestClass;
    }

    @SpxServiceCommunicationDoc(methodName = "listMethod", description = "")
    public List<SomeTestClass> listMethod() {
        SomeTestClass someTestClass = new SomeTestClass();
        someTestClass.setFoo("bar");
        return Collections.singletonList(someTestClass);
    }

    @SpxServiceCommunicationDoc(methodName = "voidMethod", description = "")
    public void voidMethod() {
        logger.info("void method called in test");
    }

    @SpxServiceCommunicationDoc(methodName = "paramMethod", description = "")
    public void paramMethod(String param) {
        logger.info("paramMethod method called in test. param {}", param);
    }

    @SpxServiceCommunicationDoc(methodName = "returnClassWithMap", description = "")
    public ClassWithAMap returnClassWithMap() {
        logger.info("returnClassWithMap method called in test.");
        ClassWithAMap cl = new ClassWithAMap();
        cl.setName("foo");

        return cl;
    }

    @SpxServiceCommunicationDoc(methodName = "returnPageClassWithMap", description = "")
    public Page<ClassWithAMap> returnPageClassWithMap() {
        logger.info("returnPageClassWithMap method called in test.");
        ClassWithAMap cl = new ClassWithAMap();
        cl.setName("foo");
        cl.setMetadata(new HashMap<>());

        List<ClassWithAMap> list = new ArrayList<>();
        list.add(cl);

        return new PageImpl(list);
    }

    @SpxServiceCommunicationDoc(methodName = "returnSetClassWithMap", description = "")
    public Set<ClassWithAMap> returnSetClassWithMap() {
        logger.info("returnSetClassWithMap method called in test.");
        ClassWithAMap cl = new ClassWithAMap();
        cl.setName("foo");
        cl.setMetadata(new HashMap<>());

        Set<ClassWithAMap> set = new HashSet<>();
        set.add(cl);

        return set;
    }

    @SpxServiceCommunicationDoc(methodName = "returnListClassWithMap", description = "")
    public List<ClassWithAMap> returnListClassWithMap() {
        logger.info("returnListClassWithMap method called in test.");
        ClassWithAMap cl = new ClassWithAMap();
        cl.setName("foo");
        cl.setMetadata(new HashMap<>());

        List<ClassWithAMap> list = new ArrayList<>();
        list.add(cl);

        return list;
    }

    @SpxServiceCommunicationDoc(methodName = "contextTest", description = "")
    public Map<String, String> contextTest() {
        logger.info("contextTest method called in test.");
        HashMap<String, String> result = new HashMap<>();
        result.put("user_id", RpcContext.getUserId());
        result.put("caller_class", RpcContext.getCallerClass());

        return result;
    }
    
    @SpxServiceCommunicationDoc(methodName = "returnEmptyList", description = "")
    public List<String> returnEmptyList() {
        logger.info("returnEmptyList method called in test.");
        return new ArrayList<>();
    }

    @SpxServiceCommunicationDoc(methodName = "receiveMap", description = "")
    public UUID receiveMap(Map<UUID, Long> map) {
        logger.info("receiveMap method called in test.");
        UUID result = null;
        for(Map.Entry<UUID, Long> entry : map.entrySet()) {
            result = entry.getKey();
        }

        return result;
    }

    @SpxServiceCommunicationDoc(methodName = "fakeSave", description = "")
    public DeviceForTest fakeSave(DeviceForTest device) throws InterruptedException {
        Thread.sleep(1000);

        return device;
    }

    @SpxServiceCommunicationDoc(methodName = "fakeFind", description = "", cacheName = TestCache.Constants.FAKE_DEVICE_CACHE)
    public DeviceForTest fakeFind(UUID deviceId)  {
        DeviceForTest device = new DeviceForTest();
        device.setId(deviceId);

        spxCacheManager.set(deviceId, device, TestCache.FAKE_DEVICE_CACHE);

        return device;
    }
}
