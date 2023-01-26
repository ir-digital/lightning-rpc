package de.seepex.componenttest


import de.seepex.ServiceDocApplication
import de.seepex.domain.Param
import de.seepex.domain.User
import de.seepex.service.BaseJsonRpcConnector
import de.seepex.servicedoc.config.TestEmbeddedRedisConfiguration
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

@ContextConfiguration(classes = ServiceDocApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = TestEmbeddedRedisConfiguration.class)
@Import(IntegrationConfiguration_v6.class)
abstract class ComponentTestSpecification extends Specification {

    @Autowired
    WebApplicationContext webApplicationContext

    @Autowired
    AmqpTemplate amqpTemplate

    @Autowired
    BaseJsonRpcConnector baseJsonRpcConnector

    final UUID tenantId = UUID.fromString("bb797475-b328-43fb-9d16-27cb235ac14d")

    def setup() {
    }

    User configureSecurityContext(List<String> roles, List<UUID> groups = []) {
        User user = new User()
        user.setId(UUID.randomUUID())
        user.setUsername("testUser")
        user.setRoles(roles)
        user.setTenantId(tenantId)

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        SecurityContext securityContext = SecurityContextHolder.getContext()
        securityContext.setAuthentication(authentication)

        return user
    }

    void deleteUser(UUID id) {
        baseJsonRpcConnector.rpc("delete", "user-service", new Param("id", id))
    }

}
