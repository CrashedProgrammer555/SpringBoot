package mo.daoyi.spring.configs;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Slf4j
@Configuration
@Data
public class JedisConfigs {

    @Value("${spring.redis.host}")
    private String host;

    @Value("@{spring.redis.port}")
    private int port;

    @Value("@{spring.redis.password}")
    private String password;

    @Value("@{spring.redis.jedis.pool.max-idle}")
    private int maxIdle;

    @Value("@{spring.redis.jedis.pool.max-active}")
    private int maxActive;

    @Value("@{spring.redis.jedis.pool.min-idle}")
    private int minIdle;

    @Value("@{spring.redis.time-out}")
    private int timeOut;

    @Bean
    public JedisPool jedisPool(){
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMaxTotal(maxActive);

        JedisPool jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut,password);

        log.info("redis已连接");

        return jedisPool;
    }
}
