package com.sequenceiq.cloudbreak.cloud.aws

import com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashSet

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.api.model.InstanceProfileStrategy
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
class AwsPlatformParameters : PlatformParameters {

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private val awsVmParameterDefinitionPath: String? = null

    @Value("${cb.aws.zone.parameter.definition.path:}")
    private val awsZoneParameterDefinitionPath: String? = null

    private var regions: Map<Region, List<AvailabilityZone>> = HashMap()
    private var vmTypes: Map<AvailabilityZone, List<VmType>> = HashMap()
    private var defaultRegion: Region? = null
    private var defaultVmType: VmType? = null

    @PostConstruct
    fun init() {
        this.regions = readRegions()
        this.vmTypes = readVmTypes()
        this.defaultRegion = nthElement(this.regions.keys, DEFAULT_REGION_TYPE_POSITION)
        this.defaultVmType = nthElement(this.vmTypes[this.vmTypes.keys.iterator().next()], DEFAULT_VM_TYPE_POSITION)
    }

    private fun readVmTypes(): Map<AvailabilityZone, List<VmType>> {
        val vmTypes = HashMap<AvailabilityZone, List<VmType>>()
        val tmpVmTypes = ArrayList<VmType>()
        val vm = getDefinition(awsVmParameterDefinitionPath, "vm")
        try {
            val oVms = JsonUtil.readValue<VmsSpecification>(vm, VmsSpecification::class.java)
            for (vmSpecification in oVms.items!!) {

                val builder = VmTypeMeta.VmTypeMetaBuilder.builder().withCpuAndMemory(vmSpecification.metaSpecification!!.properties!!.cpu,
                        vmSpecification.metaSpecification!!.properties!!.memory)

                for (configSpecification in vmSpecification.metaSpecification!!.configSpecification!!) {
                    addConfig(builder, configSpecification)
                }
                val vmTypeMeta = builder.create()
                tmpVmTypes.add(VmType.vmTypeWithMeta(vmSpecification.value, vmTypeMeta))
            }
            Collections.sort(tmpVmTypes, StringTypesCompare())
            for (regionListEntry in regions.entries) {
                for (availabilityZone in regionListEntry.value) {
                    vmTypes.put(availabilityZone, tmpVmTypes)
                }
            }
        } catch (e: IOException) {
            return vmTypes
        }

        return sortMap(vmTypes)
    }

    private fun addConfig(builder: VmTypeMeta.VmTypeMetaBuilder, configSpecification: ConfigSpecification) {
        if (configSpecification.volumeParameterType == VolumeParameterType.AUTO_ATTACHED.name) {
            builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification))
        } else if (configSpecification.volumeParameterType == VolumeParameterType.EPHEMERAL.name) {
            builder.withEphemeralConfig(volumeParameterConfig(configSpecification))
        } else if (configSpecification.volumeParameterType == VolumeParameterType.MAGNETIC.name) {
            builder.withMagneticConfig(volumeParameterConfig(configSpecification))
        } else if (configSpecification.volumeParameterType == VolumeParameterType.SSD.name) {
            builder.withSsdConfig(volumeParameterConfig(configSpecification))
        } else if (configSpecification.volumeParameterType == VolumeParameterType.ST1.name) {
            builder.withSt1Config(volumeParameterConfig(configSpecification))
        }
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
        val zone = getDefinition(awsZoneParameterDefinitionPath, "zone")
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

    private fun diskMappings(): Map<String, VolumeParameterType> {
        val map = HashMap<String, VolumeParameterType>()
        map.put(AwsDiskType.Standard.value, VolumeParameterType.MAGNETIC)
        map.put(AwsDiskType.Gp2.value, VolumeParameterType.SSD)
        map.put(AwsDiskType.Ephemeral.value, VolumeParameterType.EPHEMERAL)
        map.put(AwsDiskType.St1.value, VolumeParameterType.ST1)
        return map
    }

    private val diskTypes: Collection<DiskType>
        get() {
            val disks = Lists.newArrayList<DiskType>()
            for (diskType in AwsDiskType.values()) {
                disks.add(Companion.diskType(diskType.value))
            }
            return disks
        }

    private fun defaultDiskType(): DiskType {
        return Companion.diskType(AwsDiskType.Standard.value())
    }

    override fun regions(): Regions {
        return Regions(regions.keys, defaultRegion)
    }

    override fun availabilityZones(): AvailabilityZones {
        return AvailabilityZones(regions)
    }

    override fun resourceDefinition(resource: String): String {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/aws-$resource.json")
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        val additionalStackParameterValidations = Lists.newArrayList<StackParamValidation>()
        additionalStackParameterValidations.add(StackParamValidation(DEDICATED_INSTANCES, false, Boolean::class.java, Optional.absent<String>()))
        additionalStackParameterValidations.add(StackParamValidation(INSTANCE_PROFILE_STRATEGY, false, InstanceProfileStrategy::class.java,
                Optional.absent<String>()))
        additionalStackParameterValidations.add(StackParamValidation(S3_ROLE, false, String::class.java, Optional.absent<String>()))
        return additionalStackParameterValidations
    }

    override fun vmTypes(): VmTypes {
        val lists = LinkedHashSet<VmType>()
        for (vmTypeList in vmTypes.values) {
            lists.addAll(vmTypeList)
        }
        return VmTypes(lists, defaultVmType)
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList<T>(Companion.orchestrator(OrchestratorConstants.SALT)), Companion.orchestrator(OrchestratorConstants.SALT))
    }

    enum class AwsDiskType private constructor(private val value: String) {
        Standard("standard"),
        Ephemeral("ephemeral"),
        Gp2("gp2"),
        St1("st1");

        fun value(): String {
            return value
        }
    }

    companion object {
        val EBS_MAGNETIC_CONFIG = VolumeParameterConfig(VolumeParameterType.MAGNETIC, 1, 1024, 1, 24)
        val EBS_SSD_CONFIG = VolumeParameterConfig(VolumeParameterType.SSD, 1, 17592, 1, 24)
        val DEDICATED_INSTANCES = "dedicatedInstances"
        val INSTANCE_PROFILE_STRATEGY = "instanceProfileStrategy"
        val S3_ROLE = "s3Role"

        private val START_LABEL = Integer.valueOf(97)
        private val SCRIPT_PARAMS = ScriptParams("xvd", START_LABEL)
        private val DEFAULT_REGION_TYPE_POSITION = 4
        private val DEFAULT_VM_TYPE_POSITION = 21
    }

}
