package com.sequenceiq.cloudbreak.converter.util

import com.google.api.client.util.Lists
import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

object PlatformConverterUtil {

    fun <T : StringType> convertDefaults(vms: Map<Platform, T>): Map<String, String> {
        val result = Maps.newHashMap<String, String>()
        for (entry in vms.entries) {
            result.put(entry.key.value(), entry.value.value())
        }
        return result
    }

    fun <P : StringType, T : StringType, C : Collection<T>> convertPlatformMap(vms: Map<P, C>): Map<String, Collection<String>> {
        val result = Maps.newHashMap<String, Collection<String>>()
        for (entry in vms.entries) {
            result.put(entry.key.value(), convertList(entry.value))
        }
        return result
    }

    fun <T : StringType> convertList(vmlist: Collection<T>): Collection<String> {
        val result = Lists.newArrayList<String>()
        for (item in vmlist) {
            result.add(item.value())
        }
        return result
    }
}
