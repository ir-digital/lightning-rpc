package de.seepex.componenttest.service

import de.seepex.componenttest.ComponentTestSpecification
import de.seepex.domain.ClassWithAMap
import de.seepex.domain.Param
import de.seepex.domain.RpcResponse
import de.seepex.domain.SomeTestClass
import de.seepex.infrastructure.exception.NotSupportedException
import de.seepex.service.BaseJsonRpcConnectorV2
import de.seepex.service.RpcEventService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page

class BaseJsonRpcConnectorV2Spec extends ComponentTestSpecification {

    @Autowired
    private BaseJsonRpcConnectorV2 baseJsonRpcConnectorV2

    @Autowired
    private RpcEventService rpcEventService

    def "should be able to make rpc call"() {
        when:
        RpcResponse result = baseJsonRpcConnectorV2.rpc("stringMethod", "integrationtest-service")

        then:
        result
        result.getResponse() == "ok ok ok"
        noExceptionThrown()
    }

    def "should make calls that return objects"() {
        when:
        RpcResponse<SomeTestClass> result = baseJsonRpcConnectorV2.rpc("fooMethod", "integrationtest-service")

        then:
        result
        result.getResponse().getFoo() == "bar"
        noExceptionThrown()
    }

    def "should be able to transport exception data in headers instead of timing out"() {
        when:
        RpcResponse result = baseJsonRpcConnectorV2.rpc("exceptionThrowingMethod", "integrationtest-service")

        then:
        result
        result.isFailed()
        result.exceptionText == "i am dead"
        result.exceptionClassName == "java.lang.Exception"
        noExceptionThrown()
    }

    def "should execute void methods"() {
        when:
        RpcResponse result = baseJsonRpcConnectorV2.rpc("voidMethod", "integrationtest-service")

        then:
        result
        !result.isFailed()
        noExceptionThrown()
    }

    def "should retrieve class with a empty map"() {
        when:
        RpcResponse<ClassWithAMap> result = baseJsonRpcConnectorV2.rpc("returnClassWithMap", "integrationtest-service")

        then:
        result.getResponse().getName() == "foo"
        noExceptionThrown()
    }

    def "should retrieve a page of classes with a empty map"() {
        when:
        RpcResponse<Page<ClassWithAMap>> result = baseJsonRpcConnectorV2.rpc("returnPageClassWithMap", "integrationtest-service")

        then:
        result.getResponse().getContent().get(0).getName() == "foo"
        noExceptionThrown()
    }

    def "should retrieve a set of classes with a map"() {
        when:
        RpcResponse<Set<ClassWithAMap>> result = baseJsonRpcConnectorV2.rpc("returnSetClassWithMap", "integrationtest-service")

        then:
        result.getResponse().asList().get(0).name == "foo"
        noExceptionThrown()
    }

    def "should retrieve a list of classes with a map"() {
        when:
        RpcResponse<List<ClassWithAMap>> result = baseJsonRpcConnectorV2.rpc("returnListClassWithMap", "integrationtest-service")

        then:
        result.getResponse().get(0).name == "foo"
        noExceptionThrown()
    }

    def "should return empty list as expected"() {
        when:
        RpcResponse<List<String>> result = baseJsonRpcConnectorV2.rpc("returnEmptyList", "integrationtest-service")

        then:
        result.getResponse().size() == 0
        noExceptionThrown()
    }
    
    def "should be able to receive a map with objects"() {
        given:
        UUID id = UUID.fromString("100145d1-97ef-438e-9eb1-afb95da1b1e1")

        Map<UUID, Long> map = new HashMap<>()
        map.put(id, 1L)

        when:
        RpcResponse<UUID> result = baseJsonRpcConnectorV2.rpc("receiveMap", "integrationtest-service", new Param("map", map, true))

        then:
        !result.isFailed()
        result.getResponse().equals(id)
    }

    def "should be able to receive event when a call was made"() {
        given:
        String correlationId = UUID.randomUUID().toString()

        HashMap<String, String> applicationHeaders = new HashMap<>()
        applicationHeaders.put("correlationId", correlationId)

        when:
        baseJsonRpcConnectorV2.rpc("returnEmptyList", "integrationtest-service", applicationHeaders)

        and:
        Thread.sleep(300)
        def event = rpcEventService.getEvent(correlationId)

        then:
        noExceptionThrown()
        event
    }
    
}
