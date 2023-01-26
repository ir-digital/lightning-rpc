package de.seepex


import de.seepex.domain.Param
import de.seepex.service.CacheContainer
import de.seepex.service.CacheService
import de.seepex.service.SpxCacheManager
import spock.lang.Specification
import spock.lang.Unroll

class CacheServiceSpec extends Specification {

    CacheService cacheService
    SpxCacheManager spxCacheManager = Mock()
    CacheContainer cacheContainer = Mock()

    def setup() {
        cacheService = new CacheService(spxCacheManager, cacheContainer)
    }

    @Unroll
    def "should extract cache key as expected"() {
        when:
        def result = cacheService.getCacheKey(params)

        then:
        result == expectedResult

        where:
        params                                                                | expectedResult
        [new Param("id", "xxx")] as Param[]                                   | "xxx"
        [new Param("id", "xxx"), new Param("name", "foo")] as Param[]         | "xxx|foo"
    }

}
