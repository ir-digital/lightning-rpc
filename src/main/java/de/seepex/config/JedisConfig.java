package de.seepex.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Configuration("serviceDocJedis")
public class JedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.readhost}")
    private String redisReadHost;

    @Value("${spring.redis.readport}")
    private int redisReadPort;

    @Value("${spring.redis.port}")
    private int redisPort;

    // Default zero-length if not configured in any yml
    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database}")
    private int database;

    @Value("${spring.redis.jedis.pool.max-active}")
    private int maxActive;

    @Value("${spring.redis.jedis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.redis.jedis.pool.min-idle}")
    private int minIdle;

    private static final Logger LOG = LoggerFactory.getLogger(JedisConfig.class);

    @Bean(name = "serviceDocWritePool")
    public JedisPool jedisPoolx() {
        GenericObjectPoolConfig<Jedis> pc = new GenericObjectPoolConfig();
        pc.setMinIdle(minIdle);
        pc.setMaxIdle(maxIdle);
        pc.setMaxTotal(maxActive);
        pc.setMaxWaitMillis(5000);
        LOG.info("Using " + JedisPool.class.getProtectionDomain().getCodeSource().getLocation());

        return new JedisPool(pc, redisHost, redisPort, 5000, StringUtils.isEmpty(redisPassword) ? null : redisPassword, database);
    }

    @Bean(name = "serviceDocReadPool")
    public JedisPool jedisReadPool() {
        LOG.info("Initialized READ jedisPool on {} with port {} on Database {}", redisHost, redisPort, database);
        GenericObjectPoolConfig<Jedis> pc = new GenericObjectPoolConfig();
        pc.setMinIdle(minIdle);
        pc.setMaxIdle(maxIdle);
        pc.setMaxTotal(maxActive);
        pc.setMaxWaitMillis(5000);
        LOG.info("Using " + JedisPool.class.getProtectionDomain().getCodeSource().getLocation());

        return new JedisPool(pc, redisReadHost, redisReadPort, 5000, StringUtils.isEmpty(redisPassword)? null : redisPassword, database);
    }

}
