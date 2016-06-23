package com.sequenceiq.cloudbreak.cloud.init

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform
import java.util.HashMap

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.google.common.base.Strings
import com.google.common.collect.HashMultimap
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Component
class CloudPlatformConnectors {

    @Value("${cb.platform.default.variants:}")
    private val platformDefaultVariants: String? = null

    private val defaultVariants = HashMap<Platform, Variant>()

    @Inject
    private val cloudConnectors: List<CloudConnector>? = null
    private val map = HashMap<CloudPlatformVariant, CloudConnector>()
    private var platformToVariants: Multimap<Platform, Variant>? = null

    @PostConstruct
    fun cloudPlatformConnectors() {
        platformToVariants = HashMultimap.create<Platform, Variant>()
        for (connector in cloudConnectors!!) {
            map.put(CloudPlatformVariant(connector.platform(), connector.variant()), connector)
            platformToVariants!!.put(connector.platform(), connector.variant())
        }
        val environmentDefaults = extractEnvironmentDefaultVariants()
        setupDefaultVariants(platformToVariants, environmentDefaults)
        LOGGER.debug(map.toString())
        LOGGER.debug(defaultVariants.toString())
    }

    private fun extractEnvironmentDefaultVariants(): Map<Platform, Variant> {
        return toMap(platformDefaultVariants)
    }

    private fun toMap(s: String): Map<Platform, Variant> {
        val result = Maps.newHashMap<Platform, Variant>()
        if (!Strings.isNullOrEmpty(s)) {
            for (entry in s.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                val keyValue = entry.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                result.put(Companion.platform(keyValue[0]), Variant.variant(keyValue[1]))
            }
        }
        return result
    }

    private fun setupDefaultVariants(platformToVariants: Multimap<Platform, Variant>, environmentDefaults: Map<Platform, Variant>) {
        for (platformVariants in platformToVariants.asMap().entries) {
            if (platformVariants.value.size == 1) {
                defaultVariants.put(platformVariants.key, platformVariants.value.toArray<Variant>(arrayOf<Variant>())[0])
            } else {
                if (platformVariants.value.contains(environmentDefaults[platformVariants.key])) {
                    defaultVariants.put(platformVariants.key, environmentDefaults[platformVariants.key])
                } else {
                    throw IllegalStateException(String.format("No default variant is specified for platform: '%s'", platformVariants.key))
                }
            }
        }
    }

    fun getDefaultVariant(platform: Platform): Variant {
        return defaultVariants[platform]
    }

    fun getDefault(platform: Platform): CloudConnector {
        val variant = getDefaultVariant(platform)
        return map[CloudPlatformVariant(platform, variant)]
    }

    operator fun get(platform: Platform, variant: Variant): CloudConnector {
        return get(CloudPlatformVariant(platform, variant))
    }

    operator fun get(variant: CloudPlatformVariant): CloudConnector {
        val cc = map[variant] ?: return getDefault(variant.platform) ?: throw IllegalArgumentException(String.format("There is no cloud connector for: '%s'; available connectors: %s",
                variant, map.keys))
        return cc
    }

    val platformVariants: PlatformVariants
        get() = PlatformVariants(platformToVariants!!.asMap(), defaultVariants)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudPlatformConnectors::class.java)
    }

}
