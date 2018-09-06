package top.ibase4j.core.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import top.ibase4j.core.support.cache.RedisHelper;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.InstanceUtil;
import top.ibase4j.core.util.PropertiesUtil;

@Configuration
@ConditionalOnClass(RedisCacheConfiguration.class)
public class RedisConfig {
    @Bean
    public GenericObjectPoolConfig redisPoolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMinIdle(PropertiesUtil.getInt("redis.minIdle"));
        poolConfig.setMaxIdle(PropertiesUtil.getInt("redis.maxIdle"));
        poolConfig.setMaxTotal(PropertiesUtil.getInt("redis.maxTotal"));
        poolConfig.setMaxWaitMillis(PropertiesUtil.getInt("redis.maxWaitMillis"));
        return poolConfig;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ClientResources.class)
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(GenericObjectPoolConfig redisPoolConfig,
        ClientResources clientResources) {
        LettuceConnectionFactory connectionFactory;
        String nodes = PropertiesUtil.getString("redis.cluster.nodes");
        String master = PropertiesUtil.getString("redis.master");
        String sentinels = PropertiesUtil.getString("redis.sentinels");
        Duration commandTimeout = Duration.ofMillis(PropertiesUtil.getInt("redis.commandTimeout", 60000));
        Duration shutdownTimeout = Duration.ofMillis(PropertiesUtil.getInt("redis.shutdownTimeout", 5000));
        LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder()
                .poolConfig(redisPoolConfig).commandTimeout(commandTimeout).shutdownTimeout(shutdownTimeout)
                .clientResources(clientResources);
        LettuceClientConfiguration clientConfiguration = builder.build();
        RedisPassword password = RedisPassword.of(PropertiesUtil.getString("redis.password"));
        String host = PropertiesUtil.getString("redis.host", "localhost");
        Integer port = PropertiesUtil.getInt("redis.port", 6379);
        if (DataUtil.isNotEmpty(nodes)) {
            List<String> list = InstanceUtil.newArrayList(nodes.split(","));
            RedisClusterConfiguration configuration = new RedisClusterConfiguration(list);
            configuration.setMaxRedirects(PropertiesUtil.getInt("redis.cluster.max-redirects"));
            configuration.setPassword(password);
            connectionFactory = new LettuceConnectionFactory(configuration, clientConfiguration);
        } else if (DataUtil.isNotEmpty(master) && DataUtil.isNotEmpty(sentinels)) {
            Set<String> set = InstanceUtil.newHashSet(sentinels.split(","));
            RedisSentinelConfiguration configuration = new RedisSentinelConfiguration(master, set);
            configuration.setPassword(password);
            connectionFactory = new LettuceConnectionFactory(configuration, clientConfiguration);
        } else {
            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
            configuration.setPassword(password);
            configuration.setHostName(host);
            configuration.setPort(port);
            connectionFactory = new LettuceConnectionFactory(configuration, clientConfiguration);
        }
        return connectionFactory;
    }

    @Bean
    public RedisTemplate<Serializable, Serializable> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Serializable, Serializable> redisTemplate = new RedisTemplate<Serializable, Serializable>();
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericFastJsonRedisSerializer valueSerializer = new GenericFastJsonRedisSerializer();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        redisTemplate.setEnableTransactionSupport(new Boolean(PropertiesUtil.getString("redis.enableTransaction")));
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(PropertiesUtil.getInt("redis.cache.ttl", 10)));
        RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(configuration);
        if (new Boolean(PropertiesUtil.getString("redis.cache.enableTransaction"))) {
            builder.transactionAware();
        }
        RedisCacheManager cacheManager = builder.build();
        return cacheManager;
    }

    @Bean
    public RedisHelper redisHelper(RedisConnectionFactory redisConnectionFactory) {
        return redisHelper(redisTemplate(redisConnectionFactory));
    }

    public RedisHelper redisHelper(RedisTemplate<Serializable, Serializable> redisTemplate) {
        return new RedisHelper(redisTemplate);
    }
}
