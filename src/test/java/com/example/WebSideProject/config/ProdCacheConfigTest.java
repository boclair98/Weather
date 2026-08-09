package com.example.WebSideProject.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import static org.assertj.core.api.Assertions.assertThat;

class ProdCacheConfigTest {

    @Test
    void usesLocalBoundedCachesSoRedisCannotBlockRequestCaching() {
        CacheManager manager = new ProdCacheConfig().cacheManager();

        assertThat(manager.getCache("weather")).isInstanceOf(CaffeineCache.class);
        assertThat(manager.getCache("plannerSource")).isInstanceOf(CaffeineCache.class);
        assertThat(manager.getCache("locations")).isInstanceOf(CaffeineCache.class);
    }
}
