package com.sequenceiq.cloudbreak.cloud.aws.cache

import java.lang.reflect.Method

import org.springframework.cache.interceptor.SimpleKey
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cache.CacheDefinition
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView

import net.sf.ehcache.config.CacheConfiguration

@Service
class AwsCachingConfig : CacheDefinition {

    override fun cacheConfiguration(): CacheConfiguration {
        val cacheConfiguration = CacheConfiguration()
        cacheConfiguration.maxEntriesLocalHeap = MAX_ENTRIES
        cacheConfiguration.name = TEMPORARY_AWS_CREDENTIAL_CACHE
        cacheConfiguration.timeToLiveSeconds = TTL_IN_SECONDS
        return cacheConfiguration
    }

    override fun generate(target: Any, method: Method, vararg params: Any): Any {
        if (params.size == 0) {
            return SimpleKey.EMPTY
        }
        if (params.size == 1) {
            val param = params[0] as AwsCredentialView
            if (param.id != null) {
                return param.id
            } else {
                return SimpleKey.EMPTY
            }
        }
        return SimpleKey.EMPTY
    }

    override fun type(): Class<Any> {
        return AwsCredentialView::class.java
    }

    companion object {

        val TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential"
        private val TTL_IN_SECONDS = 5L * 60
        private val MAX_ENTRIES = 1000L
    }
}
