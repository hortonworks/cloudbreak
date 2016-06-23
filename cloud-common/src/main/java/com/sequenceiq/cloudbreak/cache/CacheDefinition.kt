package com.sequenceiq.cloudbreak.cache

import java.lang.reflect.Method

import net.sf.ehcache.config.CacheConfiguration

interface CacheDefinition {

    fun cacheConfiguration(): CacheConfiguration

    fun generate(target: Any, method: Method, vararg params: Any): Any

    fun type(): Class<Any>
}
