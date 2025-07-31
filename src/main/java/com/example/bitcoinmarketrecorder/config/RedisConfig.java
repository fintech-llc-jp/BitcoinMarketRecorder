package com.example.bitcoinmarketrecorder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.database:0}")
    private int database;

    @Bean
    public io.lettuce.core.RedisClient customRedisClient() {
        // RedisURIでライブラリ情報を無効化してCLIENT SETINFOコマンドを回避
        io.lettuce.core.RedisURI.Builder builder = io.lettuce.core.RedisURI.Builder
            .redis(host, port)
            .withDatabase(database)
            .withLibraryName("")
            .withLibraryVersion("");
        
        if (password != null && !password.trim().isEmpty()) {
            builder.withPassword(password.toCharArray());
        }
        
        io.lettuce.core.RedisURI redisUri = builder.build();
        
        // カスタムClientOptionsでCLIENT SETINFOを無効化
        io.lettuce.core.ClientOptions clientOptions = io.lettuce.core.ClientOptions.builder()
            .pingBeforeActivateConnection(false)
            .publishOnScheduler(false)
            .autoReconnect(true)
            .build();
        
        io.lettuce.core.RedisClient redisClient = io.lettuce.core.RedisClient.create(redisUri);
        redisClient.setOptions(clientOptions);
        
        return redisClient;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(database);
        
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        }
        
        // LettuceのクライアントオプションでCLIENT SETINFOを無効化
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(io.lettuce.core.ClientOptions.builder()
                .pingBeforeActivateConnection(false)
                .publishOnScheduler(false)
                .autoReconnect(true)
                .build())
            .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        
        // ライフサイクル管理の問題を回避するため手動で初期化
        factory.setEagerInitialization(true);
        factory.afterPropertiesSet();
        factory.start();
        
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}