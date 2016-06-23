package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.google.api.client.util.Lists
import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.api.model.VmTypeMetaJson
import com.sequenceiq.cloudbreak.api.model.VolumeParameterConfigJson
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson
import com.sequenceiq.cloudbreak.api.model.VmTypeJson
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil

@Component
class PlatformVirtualMachineTypesToJsonConverter : AbstractConversionServiceAwareConverter<PlatformVirtualMachines, PlatformVirtualMachinesJson>() {

    override fun convert(source: PlatformVirtualMachines): PlatformVirtualMachinesJson {
        val json = PlatformVirtualMachinesJson()
        json.defaultVirtualMachines = PlatformConverterUtil.convertDefaults(source.defaultVirtualMachines)
        json.virtualMachines = convertVmMap(source.virtualMachines)
        return json
    }

    fun convertVmMap(vms: Map<Platform, Collection<VmType>>): Map<String, Collection<VmTypeJson>> {
        val result = Maps.newHashMap<String, Collection<VmTypeJson>>()
        for (entry in vms.entries) {
            result.put(entry.key.value(), convertVmList(entry.value))
        }
        return result
    }

    fun convertVmList(vmlist: Collection<VmType>): Collection<VmTypeJson> {
        val result = Lists.newArrayList<VmTypeJson>()
        for (item in vmlist) {
            val vmTypeMetaJson = VmTypeMetaJson()
            vmTypeMetaJson.properties = item.metaData!!.properties
            val autoAttachedConfig = item.metaData!!.autoAttachedConfig
            if (autoAttachedConfig != null) {
                val volumeParameterConfigJson = VolumeParameterConfigJson()
                volumeParameterConfigJson.volumeParameterType = autoAttachedConfig.volumeParameterType().name
                volumeParameterConfigJson.maximumNumber = autoAttachedConfig.maximumNumber()
                volumeParameterConfigJson.minimumNumber = autoAttachedConfig.minimumNumber()
                volumeParameterConfigJson.maximumSize = autoAttachedConfig.maximumSize()
                volumeParameterConfigJson.minimumSize = autoAttachedConfig.minimumSize()
                vmTypeMetaJson.configs.add(volumeParameterConfigJson)
            }
            val ephemeralConfig = item.metaData!!.ephemeralConfig
            if (ephemeralConfig != null) {
                val volumeParameterConfigJson = VolumeParameterConfigJson()
                volumeParameterConfigJson.volumeParameterType = ephemeralConfig.volumeParameterType().name
                volumeParameterConfigJson.maximumNumber = ephemeralConfig.maximumNumber()
                volumeParameterConfigJson.minimumNumber = ephemeralConfig.minimumNumber()
                volumeParameterConfigJson.maximumSize = ephemeralConfig.maximumSize()
                volumeParameterConfigJson.minimumSize = ephemeralConfig.minimumSize()
                vmTypeMetaJson.configs.add(volumeParameterConfigJson)
            }
            val magneticConfig = item.metaData!!.magneticConfig
            if (magneticConfig != null) {
                val volumeParameterConfigJson = VolumeParameterConfigJson()
                volumeParameterConfigJson.volumeParameterType = magneticConfig.volumeParameterType().name
                volumeParameterConfigJson.maximumNumber = magneticConfig.maximumNumber()
                volumeParameterConfigJson.minimumNumber = magneticConfig.minimumNumber()
                volumeParameterConfigJson.maximumSize = magneticConfig.maximumSize()
                volumeParameterConfigJson.minimumSize = magneticConfig.minimumSize()
                vmTypeMetaJson.configs.add(volumeParameterConfigJson)
            }
            val ssdConfig = item.metaData!!.ssdConfig
            if (ssdConfig != null) {
                val volumeParameterConfigJson = VolumeParameterConfigJson()
                volumeParameterConfigJson.volumeParameterType = ssdConfig.volumeParameterType().name
                volumeParameterConfigJson.maximumNumber = ssdConfig.maximumNumber()
                volumeParameterConfigJson.minimumNumber = ssdConfig.minimumNumber()
                volumeParameterConfigJson.maximumSize = ssdConfig.maximumSize()
                volumeParameterConfigJson.minimumSize = ssdConfig.minimumSize()
                vmTypeMetaJson.configs.add(volumeParameterConfigJson)
            }
            val st1Config = item.metaData!!.st1Config
            if (st1Config != null) {
                val volumeParameterConfigJson = VolumeParameterConfigJson()
                volumeParameterConfigJson.volumeParameterType = st1Config.volumeParameterType().name
                volumeParameterConfigJson.maximumNumber = st1Config.maximumNumber()
                volumeParameterConfigJson.minimumNumber = st1Config.minimumNumber()
                volumeParameterConfigJson.maximumSize = st1Config.maximumSize()
                volumeParameterConfigJson.minimumSize = st1Config.minimumSize()
                vmTypeMetaJson.configs.add(volumeParameterConfigJson)
            }

            result.add(VmTypeJson(item.value(), vmTypeMetaJson))
        }
        return result
    }
}
