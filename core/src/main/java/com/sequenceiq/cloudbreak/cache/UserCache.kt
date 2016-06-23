package com.sequenceiq.cloudbreak.cache

import java.lang.reflect.Method

import org.springframework.cache.interceptor.SimpleKey
import org.springframework.stereotype.Service

import net.sf.ehcache.config.CacheConfiguration

@Service
class UserCache : CacheDefinition {

    override fun cacheConfiguration(): CacheConfiguration {
        val cacheConfiguration = CacheConfiguration()
        cacheConfiguration.name = "userCache"
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU")
        cacheConfiguration.maxEntriesLocalHeap = MAX_ENTRIES
        return cacheConfiguration
    }

    override fun generate(target: Any, method: Method, vararg params: Any): Any {
        if (params.size == 0) {
            return SimpleKey.EMPTY
        }
        if (params.size == 1) {
            val param = params[0]
            if (param != null && !param.javaClass.isArray()) {
                return param
            }
        }
        return SimpleKey(*params)
    }

    override fun type(): Class<Any> {
        return Any::class.java
    }

    companion object {

        private val MAX_ENTRIES = 1000L
    }
}
