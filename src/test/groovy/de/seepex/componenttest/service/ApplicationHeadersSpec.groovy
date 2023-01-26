package de.seepex.componenttest.service

import de.seepex.componenttest.ComponentTestSpecification
import de.seepex.domain.User

class ApplicationHeadersSpec extends ComponentTestSpecification {

    def "verify that rpc application headers are forwarded through several methods correctly"() {
        given:
        HashMap applicationheaders = new HashMap();
        applicationheaders.put("foo", "bar")

        when:
        def result = baseJsonRpcConnector.rpc("testMethod1", "integrationtest-service", applicationheaders)

        then:
        result == "{\"foo\":\"bar\"}"
    }

    def "verify that user header is set correctly"() {
        given:
        User user = configureSecurityContext([])

        when:
        def result = baseJsonRpcConnector.rpc("contextTest", "integrationtest-service")

        then:
        result
        result.get("user_id") == user.id.toString()
        noExceptionThrown()
    }
}
