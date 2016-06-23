package com.sequenceiq.cloudbreak.cache

import javax.annotation.PostConstruct
import javax.inject.Inject

import java.lang.reflect.Method
import java.util.HashMap

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
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
@EnableAutoConfiguration
class CachingConfig : CachingConfigurer {

    @Inject
    private val cacheDefinitions: List<CacheDefinition>? = null
    private val classCacheDefinitionMap = HashMap<Class<Any>, CacheDefinition>()

    @PostConstruct
    fun postCachDefinition() {
        for (cacheDefinition in cacheDefinitions!!) {
            classCacheDefinitionMap.put(cacheDefinition.type(), cacheDefinition)
        }
    }

    @Bean
    fun ehCacheManager(): net.sf.ehcache.CacheManager {
        val config = net.sf.ehcache.config.Configuration()
        for (cacheDefinition in cacheDefinitions!!) {
            config.addCache(cacheDefinition.cacheConfiguration())
        }
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
        return SpecificKeyGenerator()
    }

    override fun errorHandler(): CacheErrorHandler {
        return SimpleCacheErrorHandler()
    }

    private inner class SpecificKeyGenerator : KeyGenerator {

        override fun generate(target: Any, method: Method, vararg params: Any): Any {
            if (params.size == 0) {
                return SimpleKey.EMPTY
            }
            if (params.size == 1) {
                val cacheDefinition = classCacheDefinitionMap[params[0].javaClass]
                if (cacheDefinition == null) {
                    return classCacheDefinitionMap[Any::class.java].generate(target, method, *params)
                } else {
                    return cacheDefinition.generate(target, method, *params)
                }
            }
            return SimpleKey(*params)
        }
    }

    companion object {

        val TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential"
        private val TTL_IN_SECONDS = 5L * 60
        private val MAX_ENTRIES = 1000L
    }

}