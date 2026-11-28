package mx.edu.backendacademico.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
    @Bean
    CacheManager cacheManager() {
        return new TransactionAwareCacheManagerProxy(new ConcurrentMapCacheManager("alumnos", "alumnosLista"));
    }
}