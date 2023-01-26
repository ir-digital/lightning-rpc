package de.seepex.componenttest.service

import de.seepex.componenttest.ComponentTestSpecification
import de.seepex.domain.Alarm
import de.seepex.domain.Sensor
import de.seepex.domain.TestCacheDefinitions
import de.seepex.service.CacheResult
import de.seepex.service.SpxCacheManager
import org.springframework.beans.factory.annotation.Autowired

class SpxCacheManagerSpec extends ComponentTestSpecification {

    @Autowired
    SpxCacheManager spxCacheManager

    def "should store and get from cache"() {
        given:
        Sensor sensor = new Sensor()
        sensor.setId(UUID.randomUUID())
        sensor.setName("FOO TEST")

        when:
        spxCacheManager.set(sensor.getId(), sensor, TestCacheDefinitions.SOME_SENSOR_CACHE)

        and:
        Thread.sleep(100)
        CacheResult<Sensor> cacheResult = spxCacheManager.get(sensor.getId(), TestCacheDefinitions.SOME_SENSOR_CACHE)

        then:
        cacheResult.found
        cacheResult.getResult().id == sensor.getId()

        cleanup:
        spxCacheManager.delete(sensor.id, TestCacheDefinitions.SOME_SENSOR_CACHE)
    }

    def "should return found = false if key is not in cache"() {
        when:
        CacheResult<Sensor> cacheResult = spxCacheManager.get(UUID.randomUUID(), TestCacheDefinitions.SOME_SENSOR_CACHE)

        then:
        !cacheResult.found
    }

    def "should cache and return null as expected"() {
        given:
        UUID key = UUID.randomUUID()

        when:
        spxCacheManager.set(key, null, TestCacheDefinitions.SOME_SENSOR_CACHE)

        and:
        Thread.sleep(100)
        CacheResult<Sensor> result = spxCacheManager.get(key, TestCacheDefinitions.SOME_SENSOR_CACHE)

        then:
        result.isFound()
        result.getResult() == null

        cleanup:
        spxCacheManager.delete(key, TestCacheDefinitions.SOME_SENSOR_CACHE)
    }

    def "should cache and return empty list as expected"() {
        given:
        UUID key = UUID.randomUUID()

        when:
        spxCacheManager.set(key, new ArrayList(), TestCacheDefinitions.SOME_ALARM_CACHE)
        Thread.sleep(50)

        and:
        CacheResult<List<Alarm>> result = spxCacheManager.get(key, TestCacheDefinitions.SOME_ALARM_CACHE)

        then:
        result.isFound()
        result.getResult().size() == 0

        cleanup:
        spxCacheManager.delete(key, TestCacheDefinitions.SOME_ALARM_CACHE)
    }
}
