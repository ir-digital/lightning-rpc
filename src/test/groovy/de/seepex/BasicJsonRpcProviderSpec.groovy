package de.seepex

import de.seepex.domain.Param
import de.seepex.domain.RpcRequest
import de.seepex.service.BasicJsonRpcProvider
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class BasicJsonRpcProviderSpec extends Specification {

    ApplicationContext applicationContext

    private BasicJsonRpcProvider rpcProvider

    def setup() {
        applicationContext = Mock()

        rpcProvider = new BasicJsonRpcProvider(applicationContext)
    }

    def "find right spx method"() {
        when:
        def method = rpcProvider.getMethod(TestClass.class, "getMap")

        then:
        method.getName() == "getMap"
        method.getReturnType() == HashMap.class
    }
    
    def "should try to execute rpc call even when parameter name is wrong"() {
        given:
        RpcRequest request = new RpcRequest()
        request.addParam(new Param("wrong-param-name1", "a"))
        request.addParam(new Param("wrong-param-name2", "b"))

        and:
        def method = rpcProvider.getMethod(TestClass.class, "someMethod")

        when:
        List<Object> result = rpcProvider.getArguments(method, request)

        then:
        result[0] == "a"
        result[1] == "b"
    }

    def "getArguments should perform with a List of maps"() {
        given:
        def method = rpcProvider.getMethod(TestClass.class, "updateFeatures")

        and:
        HashMap<String, Object> feature = new HashMap<>()
        feature.put("name", "model_creation")
        feature.put("autoCreateModel", true)
        feature.put("autoCreateModelDetail", true)
        feature.put("regex", "^(?:[^-]+;){0}([^-]+)")

        and:
        RpcRequest request = new RpcRequest()
        request.addParam(new Param("tenantId", UUID.randomUUID()))
        request.addParam(new Param("features", [feature]))

        when:
        rpcProvider.getArguments(method, request)

        then:
        noExceptionThrown()
    }

    def "when returning list of interfaces, hints should be set in header in order to construct correct classes"() {
        given:
        RpcRequest request = new RpcRequest()
        request.setMethod("returnStuff")
        request.setServiceId("foo-service")
        request.addParam(new Param("tenantId", UUID.randomUUID()))

        when:
        def result = rpcProvider.callMethod(request)

        then:
        noExceptionThrown()
        1* applicationContext.getBeanDefinitionNames() >> "T"
        1* applicationContext.getBean(_) >> new TestClass()
        !result.getResponseTypeHints().isEmpty()
        result.getResponseTypeHints().get(0) == "de.seepex.domain.SomeTestClass"
        result.getResponseTypeHints().get(1) == "de.seepex.domain.SomeOtherTestClass"
    }
}
