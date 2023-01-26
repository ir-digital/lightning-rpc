package de.seepex.service

import com.google.gson.Gson
import de.seepex.domain.BackReferenceDomain
import de.seepex.domain.Param
import de.seepex.domain.RpcRequest
import spock.lang.Specification

class JsonRpcServiceSpec extends Specification {

    private JsonRpcService jsonRpcService

    def setup() {
        jsonRpcService = new JsonRpcService(null, null, null, null, null)
    }

    def "should map legacy request"() {
        given:
        String payload = "{\"service_id\":\"device-service\",\"method\":\"findByTerm\",\"params\":[{\"term\":\"**\"},{\"pageable\":{\"paged\":true,\"pageNumber\":\"0\",\"pageSize\":\"20\",\"offset\":0,\"unpaged\":false}},{\"user\":{\"id\":\"03533277-cbd3-4b83-be7f-ddab2b1ddec4\",\"username\":\"kacperwojno\",\"password\":\"xxx\",\"firstname\":\"Kacper\",\"lastname\":\"Wojno\",\"roles\":[\"ROLE_ADMIN\",\"ROLE_PLATFORM_ADMIN\"],\"rights\":[\"f846deda-c91a-4004-a887-9e1ba05629b7\",\"150f79fe-afa2-46a6-9a66-970b9ac4e1a1\"],\"isActive\":true,\"createdAt\":\"55d1e280-f924-11ec-a365-430cae761c2b\",\"lastLogin\":1662471297976061000,\"failedLogins\":0,\"tenantId\":\"a259c0d9-2770-4c7b-a420-c14e249d021b\",\"metadata\":{\"STANDARD_GROUP_2c97b40a-20a9-4ee3-be55-af9f95a9dda2\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_3c540e6a-c510-4d9a-96e2-5051f223e6ed\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_402e1452-c39e-4096-874e-785e6ccd701e\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_46bb6148-f726-44cd-9cb1-af85b4754b98\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_9514e8a7-0bf8-42aa-baf1-0635d0fb962f\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_a259c0d9-2770-4c7b-a420-c14e249d021b\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_b2bef5e8-021b-4313-8066-e6c90decda10\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_b30e03b0-f18f-44ff-a5ad-4e7340ab19e4\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_be87dafa-38aa-4c05-8acc-9a44080b6736\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\",\"STANDARD_GROUP_d9373dd2-33b6-4920-9201-b1533320e76b\":\"12c3dd35-1dbd-46c9-946c-53e7117ce474\"}}}]}"

        and:
        RpcRequest request = new RpcRequest()
        request.setServiceId("device-service")
        request.setMethod("findByTerm")
        request.setParams([new Param()])

        when:
        jsonRpcService.updateParamsFromLegacyRequest(request, payload)

        then:
        noExceptionThrown()
        request.getParams().size() == 3
        request.getParams().get(0).getName() == "term"
    }

    def "should be able to deserialize backreferences"() {
        given:
        BackReferenceDomain child = new BackReferenceDomain()
        child.setId("B")

        BackReferenceDomain parent = new BackReferenceDomain()
        parent.setId("A")

        and:
        child.setParent(parent)
        parent.setChildren([child])

        when:
        def result = jsonRpcService.gson.toJson(child)

        then:
        noExceptionThrown()
    }

    def "should be able to deserialize a list of objects"() {
        given:
        BackReferenceDomain child = new BackReferenceDomain()
        child.setId("B")

        BackReferenceDomain parent = new BackReferenceDomain()
        parent.setId("A")

        BackReferenceDomain anotherOne = new BackReferenceDomain()
        anotherOne.setId("A")

        and:
        child.setParent(parent)
        parent.setChildren([child])

        when:
        def result = jsonRpcService.gson.toJson([child, parent, anotherOne])
        def result2 = jsonRpcService.gson.toJson([child, parent, anotherOne])

        then:
        noExceptionThrown()
        new Gson().fromJson(result, List.class).size() == 3
        new Gson().fromJson(result2, List.class).size() == 3
    }

    def "should deserialize empty list"() {
        when:
        def result = jsonRpcService.gson.toJson(new ArrayList())

        then:
        noExceptionThrown()
    }
}
