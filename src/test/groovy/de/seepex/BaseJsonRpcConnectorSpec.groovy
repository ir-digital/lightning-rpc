package de.seepex

import de.seepex.domain.GenericResponse
import de.seepex.domain.Headers
import de.seepex.domain.RpcMappedClass
import de.seepex.service.BaseJsonRpcConnector
import de.seepex.service.ClassMappingHolder
import de.seepex.service.RpcTools
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import spock.lang.Specification

class BaseJsonRpcConnectorSpec extends Specification {

    private RpcTools rpcTools
    private BaseJsonRpcConnector baseJsonRpcConnector

    def setup() {
        baseJsonRpcConnector = new BaseJsonRpcConnector()
        rpcTools = Mock()
        baseJsonRpcConnector.rpcTools = rpcTools
    }

    def "handleResponse should return a map if return class is unknown"() {
        Map<String, Object> headers = new HashMap<>()
        headers.put(Headers.INVOKED_METHOD_RETURN_TYPE.name(), "SomeUnknownClass")
        headers.put(Headers.RPC_RTT.name(), 10L)
        headers.put(Headers.RPC_INVOKED_AT.name(), 12L)

        given:
        MessageProperties messageProperties = Mock()
        messageProperties.getHeaders() >> headers

        and:
        Message message = Mock()
        message.getBody() >> '{"foo":"bar"}'
        message.getMessageProperties() >> messageProperties

        when:
        GenericResponse result = baseJsonRpcConnector.handleResponse(message, 0, "service", "methid")

        then:
        1* rpcTools.decompress(_) >> '{"foo":"bar"}'
        1* rpcTools.getGenericResponse('{"foo":"bar"}')
        noExceptionThrown()
    }

    def "sould be able to map response, if SpxMappedClass annotation was used"() {
        given:
        Map<String, Object> headers = new HashMap<>()
        headers.put(Headers.INVOKED_METHOD_RETURN_TYPE.name(), "domain.foo.SomeClassWeDontKnow")
        headers.put(Headers.RPC_RTT.name(), 10L)
        headers.put(Headers.RPC_INVOKED_AT.name(), 12L)

        and:
        MessageProperties messageProperties = Mock()
        messageProperties.getHeaders() >> headers

        and:
        Message message = Mock()
        message.getBody() >> '{"foo":"bar"}'
        message.getMessageProperties() >> messageProperties

        and:
        ClassMappingHolder.getInstance().addMapping("SomeClassWeDontKnow", RpcMappedClass.class)

        when:
        def result = baseJsonRpcConnector.handleResponse(message, 0, "service", "methid")

        then:
        result
        noExceptionThrown()
        result.getFoo() == "bar"
        1* rpcTools.decompress(_) >> '{"foo":"bar"}'
    }
}
