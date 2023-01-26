package de.seepex.config;

import de.seepex.annotation.EnableSpxRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@Profile("integrationtest")
@EnableSpxRpc(service = "service-documentation")
@Configuration
public class EnableRPCServiceOnIntegrationTestEnv {
    private static final Logger LOG = LoggerFactory.getLogger(EnableRPCServiceOnIntegrationTestEnv.class);
    @PostConstruct
    protected void postConstruct() {
        LOG.warn("Enabling @EnableSpxRpc(service = \"service-documentation\")");
    }
}
