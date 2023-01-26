package de.seepex.componenttest.service

import de.seepex.DeviceForTest
import de.seepex.componenttest.ComponentTestSpecification
import de.seepex.domain.ClassWithAMap
import de.seepex.domain.Param
import de.seepex.domain.RpcEvent
import de.seepex.service.BaseJsonRpcConnector
import de.seepex.service.RpcEventService
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.CompletableFuture

class BaseJsonRpcConnectorSpec extends ComponentTestSpecification {

    @Autowired
    private BaseJsonRpcConnector baseJsonRpcConnector

    @Autowired
    private RpcEventService rpcEventService

    def "should be able to make rpc call"() {
        when:
        def result = baseJsonRpcConnector.rpc("stringMethod", "integrationtest-service")

        then:
        result == "ok ok ok"
        noExceptionThrown()
    }

    def "should make calls that return objects"() {
        when:
        def result = baseJsonRpcConnector.rpc("fooMethod", "integrationtest-service")

        then:
        result.getFoo() == "bar"
        noExceptionThrown()
    }

    def "should execute void methods"() {
        when:
        def result = baseJsonRpcConnector.rpc("voidMethod", "integrationtest-service")

        then:
        !result
        noExceptionThrown()
    }

    def "should execute param methods with null payload"() {
        when:
        def result = baseJsonRpcConnector.rpc("paramMethod", "integrationtest-service", new Param("param", null))

        then:
        !result
        noExceptionThrown()
    }

    def "should retrieve class with a empty map"() {
        when:
        ClassWithAMap result = baseJsonRpcConnector.rpc("returnClassWithMap", "integrationtest-service")

        then:
        result.getName() == "foo"
        noExceptionThrown()
    }

    def "saving with asyncRPC should work"() {
        given:
        def deviceId = UUID.randomUUID();

        DeviceForTest device = new DeviceForTest()
        device.setId(deviceId)
        device.setSerialNumber("Test_comm_" + UUID.randomUUID().toString())

        when:
        CompletableFuture<DeviceForTest> deviceAsync = baseJsonRpcConnector.rpcAsync("fakeSave", "integrationtest-service", new Param("device", device))
        assert !deviceAsync.isDone() // could be danger and sometimes break test, but I think RPC isn't so fast :)

        def deviceAfterSave = deviceAsync.get()

        then:
        deviceAfterSave.id == deviceId
        deviceAfterSave.serialNumber == device.serialNumber
    }

    def "should load from cache"() {
        given:
        DeviceForTest device = new DeviceForTest()
        device.setId(UUID.randomUUID())
        device.setSerialNumber("Test_comm_" + UUID.randomUUID().toString())

        baseJsonRpcConnector.rpc("fakeFind", "integrationtest-service", new Param("id", device.id))

        when:
        CompletableFuture<DeviceForTest> deviceAsync = baseJsonRpcConnector.rpcAsync("fakeFind", "integrationtest-service", new Param("id", device.id))
        Thread.sleep(300)
        assert deviceAsync.isDone() // loading from cache returns completed future

        def deviceFromRpc = deviceAsync.get()

        then:
        deviceFromRpc.id == device.id
    }

    def "should be able to receive event when a call was made"() {
        given:
        String correlationId = UUID.randomUUID().toString()

        HashMap<String, String> applicationHeaders = new HashMap<>()
        applicationHeaders.put("correlationId", correlationId)

        when:
        baseJsonRpcConnector.rpc("returnEmptyList", "integrationtest-service", applicationHeaders)

        and:
        Thread.sleep(300)
        def event = rpcEventService.getEvent(correlationId)

        then:
        noExceptionThrown()
        event
    }

}
