package com.sequenceiq.cloudbreak.controller.validation.template

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService

@Component
class TemplateValidator {

    @Inject
    private val cloudParameterService: CloudParameterService? = null

    fun validateTemplateRequest(value: TemplateRequest) {
        var vmType: VmType? = null
        var volumeParameterType: VolumeParameterType? = null
        val platform = Platform.platform(value.cloudPlatform)
        val virtualMachines = cloudParameterService!!.vmtypes.virtualMachines
        val diskTypes = cloudParameterService.diskTypes

        val diskMappings = diskTypes.diskMappings

        if (virtualMachines.containsKey(platform) && !virtualMachines[platform].isEmpty()) {
            for (type in virtualMachines[platform]) {
                if (type.value() == value.instanceType) {
                    vmType = type
                }
            }
            if (vmType == null) {
                throw BadRequestException(
                        String.format("The '%s' instance type isn't supported by '%s' platform", value.instanceType, platform.value()))
            }
        }

        if (diskMappings.containsKey(platform) && !diskMappings.get(platform).isEmpty()) {
            val map = diskMappings.get(platform)
            volumeParameterType = map.get(value.volumeType)
            if (volumeParameterType == null) {
                throw BadRequestException(
                        String.format("The '%s' volume type isn't supported by '%s' platform", value.volumeType, platform.value()))
            }
        }

        validateVolume(value, vmType, platform, volumeParameterType)
    }

    private fun validateVolume(value: TemplateRequest, vmType: VmType, platform: Platform, volumeParameterType: VolumeParameterType) {
        validateVolumeType(value, platform)
        validateVolumeCount(value, vmType, volumeParameterType)
        validateMaximumVolumeSize(value, vmType)
    }

    private fun validateMaximumVolumeSize(value: TemplateRequest, vmType: VmType?) {
        if (vmType != null) {
            val maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB)
            if (maxSize != null) {
                val fullSize = value.volumeSize!! * value.volumeCount!!
                if (Integer.valueOf(maxSize) < fullSize) {
                    throw BadRequestException(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.cloudPlatform, fullSize, maxSize))
                }
            }
        }
    }

    private fun validateVolumeType(value: TemplateRequest, platform: Platform) {
        val diskType = DiskType.diskType(value.volumeType)
        val diskTypes = cloudParameterService!!.diskTypes.diskTypes
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                throw BadRequestException(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()))
            }
        }
    }

    private fun validateVolumeCount(value: TemplateRequest, vmType: VmType?, volumeParameterType: VolumeParameterType) {
        if (vmType != null) {
            val config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType)
            if (config != null) {
                if (value.volumeCount > config.maximumNumber()) {
                    throw BadRequestException(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()))
                } else if (value.volumeCount < config.minimumNumber()) {
                    throw BadRequestException(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()))
                }
                if (value.volumeSize > config.maximumSize()) {
                    throw BadRequestException(String.format("Max allowed volume size for '%s': %s", vmType.value(), config.maximumSize()))
                } else if (value.volumeSize < config.minimumSize()) {
                    throw BadRequestException(String.format("Min allowed volume size for '%s': %s", vmType.value(), config.minimumSize()))
                }
            } else {
                throw BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()))
            }
        }
    }
}
