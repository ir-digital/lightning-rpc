package de.seepex.componenttest


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

class IntegrationConfiguration_v6 {

    private static final Logger LOGGER = LoggerFactory.getLogger(this.class)

    @Autowired
    private final Environment environment

    @Autowired
    private ResourceLoader resourceLoader


}
