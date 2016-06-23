package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.ConfigSpecification
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare
import com.sequenceiq.cloudbreak.cloud.model.VmSpecification
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VmsSpecification
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
class ArmPlatformParameters : PlatformParameters {

    @Value("${cb.arm.vm.parameter.definition.path:}")
    private val armVmParameterDefinitionPath: String? = null

    @Value("${cb.arm.zone.parameter.definition.path:}")
    private val armZoneParameterDefinitionPath: String? = null

    private var regions: Map<Region, List<AvailabilityZone>> = HashMap()
    private var vmTypes: List<VmType> = ArrayList()
    private var defaultRegion: Region? = null
    private var defaultVmType: VmType? = null

    @PostConstruct
    fun init() {
        this.regions = readRegions()
        this.vmTypes = readVmTypes()
        this.defaultRegion = nthElement(this.regions.keys, DEFAULT_REGION_TYPE_POSITION)
        this.defaultVmType = nthElement(this.vmTypes, DEFAULT_VM_TYPE_POSITION)
    }

    private fun readVmTypes(): List<VmType> {
        val vmTypes = ArrayList<VmType>()
        val vm = getDefinition(armVmParameterDefinitionPath, "vm")
        try {
            val oVms = JsonUtil.readValue<VmsSpecification>(vm, VmsSpecification::class.java)
            for (vmSpecification in oVms.items!!) {

                val builder = VmTypeMeta.VmTypeMetaBuilder.builder().withCpuAndMemory(vmSpecification.metaSpecification!!.properties!!.cpu,
                        vmSpecification.metaSpecification!!.properties!!.memory)

                for (configSpecification in vmSpecification.metaSpecification!!.configSpecification!!) {
                    if (configSpecification.volumeParameterType == VolumeParameterType.AUTO_ATTACHED.name) {
                        builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification))
                    } else if (configSpecification.volumeParameterType == VolumeParameterType.EPHEMERAL.name) {
                        builder.withEphemeralConfig(volumeParameterConfig(configSpecification))
                    } else if (configSpecification.volumeParameterType == VolumeParameterType.MAGNETIC.name) {
                        builder.withMagneticConfig(volumeParameterConfig(configSpecification))
                    } else if (configSpecification.volumeParameterType == VolumeParameterType.SSD.name) {
                        builder.withSsdConfig(volumeParameterConfig(configSpecification))
                    }
                }
                val vmTypeMeta = builder.create()
                vmTypes.add(VmType.vmTypeWithMeta(vmSpecification.value, vmTypeMeta))
            }
        } catch (e: IOException) {
            return vmTypes
        }

        Collections.sort(vmTypes, StringTypesCompare())
        return vmTypes
    }

    private fun volumeParameterConfig(configSpecification: ConfigSpecification): VolumeParameterConfig {
        return VolumeParameterConfig(
                VolumeParameterType.valueOf(configSpecification.volumeParameterType),
                Integer.valueOf(configSpecification.minimumSize),
                Integer.valueOf(configSpecification.maximumSize),
                Integer.valueOf(configSpecification.minimumNumber),
                Integer.valueOf(configSpecification.maximumNumberWithLimit!!))
    }

    private fun readRegions(): Map<Region, List<AvailabilityZone>> {
        val regions = HashMap<Region, List<AvailabilityZone>>()
        val zone = getDefinition(armZoneParameterDefinitionPath, "zone")
        try {
            val oRegions = JsonUtil.readValue<RegionsSpecification>(zone, RegionsSpecification::class.java)
            for (regionSpecification in oRegions.items!!) {
                val av = ArrayList<AvailabilityZone>()
                for (s in regionSpecification.zones!!) {
                    av.add(AvailabilityZone.availabilityZone(s))
                }
                Collections.sort(av, StringTypesCompare())
                regions.put(Region.region(regionSpecification.name), av)
            }
        } catch (e: IOException) {
            return regions
        }

        return sortMap(regions)
    }

    private fun getDefinition(parameter: String, type: String): String {
        if (Strings.isNullOrEmpty(parameter)) {
            return resourceDefinition(type)
        } else {
            return FileReaderUtils.readFileFromClasspathQuietly(parameter)
        }
    }

    override fun scriptParams(): ScriptParams {
        return SCRIPT_PARAMS
    }

    override fun diskTypes(): DiskTypes {
        return DiskTypes(diskTypes, defaultDiskType(), diskMappings())
    }

    override fun regions(): Regions {
        return Regions(regions.keys, defaultRegion)
    }

    override fun availabilityZones(): AvailabilityZones {
        return AvailabilityZones(regions)
    }

    private val diskTypes: Collection<DiskType>
        get() {
            val disks = Lists.newArrayList<DiskType>()
            for (diskType in ArmDiskType.values()) {
                disks.add(Companion.diskType(diskType.value()))
            }
            return disks
        }

    private fun diskMappings(): Map<String, VolumeParameterType> {
        val map = HashMap<String, VolumeParameterType>()
        map.put(ArmDiskType.GEO_REDUNDANT.value(), VolumeParameterType.MAGNETIC)
        map.put(ArmDiskType.LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC)
        map.put(ArmDiskType.PREMIUM_LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC)
        return map
    }

    private fun defaultDiskType(): DiskType {
        return Companion.diskType(ArmDiskType.LOCALLY_REDUNDANT.value())
    }

    override fun resourceDefinition(resource: String): String {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/arm-$resource.json")
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        val additionalStackParameterValidations = Lists.newArrayList<StackParamValidation>()
        additionalStackParameterValidations.add(StackParamValidation("diskPerStorage", false, String::class.java, Optional.absent<String>()))
        additionalStackParameterValidations.add(StackParamValidation("persistentStorage", false, String::class.java, Optional.of("^[a-z0-9]{0,24}$")))
        additionalStackParameterValidations.add(StackParamValidation("attachedStorageOption", false, ArmAttachedStorageOption::class.java,
                Optional.absent<String>()))
        return additionalStackParameterValidations
    }

    override fun vmTypes(): VmTypes {
        return VmTypes(vmTypes, defaultVmType)
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList<T>(Companion.orchestrator(OrchestratorConstants.SALT)), Companion.orchestrator(OrchestratorConstants.SALT))
    }

    companion object {

        private val START_LABEL = 98
        private val DEFAULT_REGION_TYPE_POSITION = 4
        private val DEFAULT_VM_TYPE_POSITION = 1
        private val SCRIPT_PARAMS = ScriptParams("sd", START_LABEL)
    }
}
