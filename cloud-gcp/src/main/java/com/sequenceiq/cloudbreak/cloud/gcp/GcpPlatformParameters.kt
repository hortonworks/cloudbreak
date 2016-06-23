package com.sequenceiq.cloudbreak.cloud.gcp

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

import com.google.api.client.util.Lists
import com.google.common.base.Strings
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.gcp.model.MachineDefinitionView
import com.sequenceiq.cloudbreak.cloud.gcp.model.MachineDefinitionWrapper
import com.sequenceiq.cloudbreak.cloud.gcp.model.ZoneDefinitionView
import com.sequenceiq.cloudbreak.cloud.gcp.model.ZoneDefinitionWrapper
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
class GcpPlatformParameters : PlatformParameters {

    @Value("${cb.gcp.vm.parameter.definition.path:}")
    private val gcpVmParameterDefinitionPath: String? = null

    @Value("${cb.gcp.zone.parameter.definition.path:}")
    private val gcpZoneParameterDefinitionPath: String? = null

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
        val vm = getDefinition(gcpVmParameterDefinitionPath, "vm")
        try {
            val machineDefinitionWrapper = JsonUtil.readValue<MachineDefinitionWrapper>(vm, MachineDefinitionWrapper::class.java)
            for (`object` in machineDefinitionWrapper.items.entries) {
                val value = `object`.value as Map<Any, Any>
                val machineTpes = value["machineTypes"] as List<Any>
                for (machineType in machineTpes) {
                    val machineDefinitionView = MachineDefinitionView(machineType as Map<Any, Any>)
                    val availabilityZone = AvailabilityZone(machineDefinitionView.zone)
                    if (!vmTypes.containsKey(availabilityZone)) {
                        val vmTypeList = ArrayList<VmType>()
                        vmTypes.put(availabilityZone, vmTypeList)
                    }
                    val vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder().withCpuAndMemory(Integer.valueOf(machineDefinitionView.guestCpus),
                            java.lang.Float.valueOf(machineDefinitionView.memoryMb) / THOUSAND).withMagneticConfig(TEN, Integer.valueOf(machineDefinitionView.maximumPersistentDisksSizeGb),
                            1, machineDefinitionView.maximumNumberWithLimit).withSsdConfig(TEN, Integer.valueOf(machineDefinitionView.maximumPersistentDisksSizeGb),
                            1, machineDefinitionView.maximumNumberWithLimit).withMaximumPersistentDisksSizeGb(machineDefinitionView.maximumPersistentDisksSizeGb).create()

                    val vmType = VmType.vmTypeWithMeta(machineDefinitionView.name, vmTypeMeta)
                    vmTypes[availabilityZone].add(vmType)
                }
            }
        } catch (e: IOException) {
            return vmTypes
        }

        for (availabilityZoneListEntry in vmTypes.entries) {
            Collections.sort(availabilityZoneListEntry.value, StringTypesCompare())
        }
        return sortMap(vmTypes)
    }

    private fun readRegions(): Map<Region, List<AvailabilityZone>> {
        val regions = HashMap<Region, List<AvailabilityZone>>()
        val zone = getDefinition(gcpZoneParameterDefinitionPath, "zone")
        try {
            val zoneDefinitionWrapper = JsonUtil.readValue<ZoneDefinitionWrapper>(zone, ZoneDefinitionWrapper::class.java)
            for (`object` in zoneDefinitionWrapper.items) {
                val region = `object`.region
                val avZone = `object`.selfLink

                val splitRegion = region.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                val splitZone = avZone.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                val regionObject = Region.region(splitRegion[splitRegion.size - 1])
                val availabilityZoneObject = AvailabilityZone.availabilityZone(splitZone[splitZone.size - 1])
                if (!regions.keys.contains(regionObject)) {
                    val availabilityZones = ArrayList<AvailabilityZone>()
                    regions.put(regionObject, availabilityZones)
                }
                regions[regionObject].add(availabilityZoneObject)

            }
        } catch (e: IOException) {
            return regions
        }

        for (availabilityZoneListEntry in regions.entries) {
            Collections.sort(availabilityZoneListEntry.value, StringTypesCompare())
        }
        return sortMap(regions)
    }

    override fun scriptParams(): ScriptParams {
        return SCRIPT_PARAMS
    }

    override fun diskTypes(): DiskTypes {
        return DiskTypes(diskTypes, defaultDiskType(), diskMappings())
    }

    private fun diskMappings(): Map<String, VolumeParameterType> {
        val map = HashMap<String, VolumeParameterType>()
        map.put(GcpDiskType.HDD.value(), VolumeParameterType.MAGNETIC)
        map.put(GcpDiskType.SSD.value(), VolumeParameterType.SSD)

        return map
    }

    private val diskTypes: Collection<DiskType>
        get() {
            val disks = Lists.newArrayList<DiskType>()
            for (diskType in GcpDiskType.values()) {
                disks.add(Companion.diskType(diskType.value()))
            }
            return disks
        }

    private fun defaultDiskType(): DiskType {
        return Companion.diskType(GcpDiskType.HDD.value())
    }

    override fun regions(): Regions {
        return Regions(regions.keys, defaultRegion)
    }

    override fun availabilityZones(): AvailabilityZones {
        return AvailabilityZones(regions)
    }

    override fun resourceDefinition(resource: String): String {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/gcp-$resource.json")
    }

    private fun getDefinition(parameter: String, type: String): String {
        if (Strings.isNullOrEmpty(parameter)) {
            return resourceDefinition(type)
        } else {
            return FileReaderUtils.readFileFromClasspathQuietly(parameter)
        }
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        return emptyList<StackParamValidation>()
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList<T>(Companion.orchestrator(OrchestratorConstants.SALT)), Companion.orchestrator(OrchestratorConstants.SALT))
    }

    override fun vmTypes(): VmTypes {
        val lists = LinkedHashSet<VmType>()
        vmTypes.values.forEach(Consumer<List<VmType>> { lists.addAll(it) })
        return VmTypes(lists, defaultVirtualMachine())
    }

    private fun defaultVirtualMachine(): VmType {
        return defaultVmType
    }

    enum class GcpDiskType private constructor(private val value: String) {
        SSD("pd-ssd"), HDD("pd-standard");

        fun value(): String {
            return value
        }

        fun getUrl(projectId: String, zone: AvailabilityZone): String {
            return getUrl(projectId, zone, value)
        }

        companion object {

            fun getUrl(projectId: String, zone: AvailabilityZone, volumeId: String): String {
                return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), volumeId)
            }
        }
    }

    companion object {
        private val DEFAULT_REGION_TYPE_POSITION = 2
        private val DEFAULT_VM_TYPE_POSITION = 14
        private val THOUSAND = 1000.0f
        private val TEN = 10
        private val START_LABEL = Integer.valueOf(97)
        private val SCRIPT_PARAMS = ScriptParams("sd", START_LABEL)
    }
}
