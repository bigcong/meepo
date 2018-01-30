package com.cc.meepo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;
import java.util.List;


@SpringBootApplication
@EnableAsync
@EnableCaching
public class MeepoApplication {
    public static List<String> chaoexList = Arrays.asList("CODE_BTC", "CODE_ETH", "CODE_DLC", "CODE_LTC");
    public static List<String> NBList = Arrays.asList("ABF_BTC", "ABF_ETH", "ABF_DLC", "ABF_LTC");


    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext run = SpringApplication.run(MeepoApplication.class, args);
        Chaoex chaoex = (Chaoex) run.getBean("chaoex");
        for (String symbol : chaoexList) {
            chaoex.go(symbol, "https://www.chaoex.com");

        }
        for (String symbol : NBList) {
            chaoex.go(symbol, "http://www.nb.top");

        }


    }

    private final RedisSerializer serializer = new StringRedisSerializer();


    @Bean
    RedisTemplate<Object, Object> redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public RedisCacheManager redisCacheManager(JedisConnectionFactory jedisConnectionFactory) {
        RedisCacheManager redisCacheManager = new RedisCacheManager(redisTemplate(jedisConnectionFactory));
        redisCacheManager.setUsePrefix(true);
        redisCacheManager.setCachePrefix(cacheName -> serializer.serialize((String.format("vienna.%s:", cacheName))));
        redisCacheManager.setDefaultExpiration(600);//10分钟的缓存
        return redisCacheManager;

    }
}
