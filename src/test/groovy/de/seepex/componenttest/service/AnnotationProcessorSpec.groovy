package de.seepex.componenttest.service

import de.seepex.componenttest.ComponentTestSpecification
import de.seepex.domain.RpcMappedClass
import de.seepex.service.ClassMappingHolder

class AnnotationProcessorSpec extends ComponentTestSpecification {


    def "should collect class mappings as expected"() {
        when:
        def result = ClassMappingHolder.getInstance().getMappedClass("SomeClassWeDontKnow")

        then:
        result
        result == RpcMappedClass.class
        noExceptionThrown()
    }
}
