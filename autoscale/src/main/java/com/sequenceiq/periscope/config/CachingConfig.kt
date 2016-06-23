package com.sequenceiq.periscope.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.ehcache.EhCacheCacheManager
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheResolver
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import net.sf.ehcache.config.CacheConfiguration

@Configuration
@EnableCaching
@EnableAutoConfiguration
class CachingConfig : CachingConfigurer {

    @Bean
    fun ehCacheManager(): net.sf.ehcache.CacheManager {
        val config = net.sf.ehcache.config.Configuration()
        config.addCache(createUserCache())
        return net.sf.ehcache.CacheManager.newInstance(config)
    }

    @Bean
    override fun cacheManager(): CacheManager {
        return EhCacheCacheManager(ehCacheManager())
    }

    override fun cacheResolver(): CacheResolver {
        return SimpleCacheResolver(cacheManager())
    }

    @Bean
    override fun keyGenerator(): KeyGenerator {
        return SimpleKeyGenerator()
    }

    override fun errorHandler(): CacheErrorHandler {
        return SimpleCacheErrorHandler()
    }

    private fun createUserCache(): CacheConfiguration {
        val cacheConfiguration = CacheConfiguration()
        cacheConfiguration.name = "userCache"
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU")
        cacheConfiguration.maxEntriesLocalHeap = MAX_ENTRIES.toLong()
        return cacheConfiguration
    }

    companion object {

        private val MAX_ENTRIES = 1000
    }

}