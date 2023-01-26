package de.seepex

import de.seepex.annotation.EnableSpxRpc
import de.seepex.service.RpcTools
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class RpcToolsSpec extends Specification {

    private ApplicationContext applicationContext
    private RpcTools rpcTools

    def setup() {
        applicationContext = Mock()
        applicationContext.getBeansWithAnnotation(_) >> new HashMap<>()
        rpcTools = new RpcTools(applicationContext)
    }

    def "should create generic response for a unknown class"() {
        when:
        def result = rpcTools.getGenericResponse('{"foo":"bar"}')

        then:
        result.getResponseType() == "java.util.LinkedHashMap"
        result.getResponse().get("foo") == "bar"
    }

    def "should create generic response for a unknown class when a map cannot be generated"() {
        when:
        def result = rpcTools.getGenericResponse('stringResponse')

        then:
        result.getResponseType() == "java.lang.String"
        result.getResponse() == "stringResponse"
    }

    def "should get hostname"() {
        when:
        def result = rpcTools.getHostName()

        then:
        noExceptionThrown()
        result
    }
}
