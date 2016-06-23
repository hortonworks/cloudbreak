package com.sequenceiq.cloudbreak.cloud.mock

import com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.LinkedHashSet

import javax.annotation.PostConstruct

import org.springframework.stereotype.Service

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants

@Service
class MockPlatformParameters : PlatformParameters {

    private enum class MockedVmTypes private constructor(private val value: String) {

        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        fun value(): String {
            return value
        }

        val vmTypeMeta: VmTypeMeta
            get() {
                val vmTypeMeta = VmTypeMeta()
                vmTypeMeta.ssdConfig = getVolumeConfig(VolumeParameterType.SSD)
                vmTypeMeta.ephemeralConfig = getVolumeConfig(VolumeParameterType.EPHEMERAL)
                vmTypeMeta.magneticConfig = getVolumeConfig(VolumeParameterType.MAGNETIC)
                vmTypeMeta.autoAttachedConfig = getVolumeConfig(VolumeParameterType.AUTO_ATTACHED)
                return vmTypeMeta
            }

        private fun getVolumeConfig(ssd: VolumeParameterType): VolumeParameterConfig {
            return VolumeParameterConfig(ssd, 1, Integer.MAX_VALUE, 0, Integer.MAX_VALUE)
        }
    }

    private var regions: Map<Region, List<AvailabilityZone>> = HashMap()
    private var vmTypes: Map<AvailabilityZone, List<VmType>> = HashMap()
    private var defaultRegion: Region? = null
    private var defaultVmType: VmType? = null

    @PostConstruct
    fun init() {
        this.regions = readRegions()
        this.vmTypes = readVmTypes()
        this.defaultRegion = this.regions.keys.iterator().next()
        this.defaultVmType = this.vmTypes[this.vmTypes.keys.iterator().next()].get(0)
    }

    private fun readRegions(): Map<Region, List<AvailabilityZone>> {
        val regions = HashMap<Region, List<AvailabilityZone>>()
        regions.put(Region.region("USA"), getAvailabilityZones(USA_AVAILABILITY_ZONES))
        regions.put(Region.region("Europe"), getAvailabilityZones(EUROPE_AVAILABILITY_ZONES))
        return regions
    }

    private fun getAvailabilityZones(availabilityZones: Array<String>): List<AvailabilityZone> {
        val availabilityZoneList = ArrayList<AvailabilityZone>()
        for (availabilityZone in availabilityZones) {
            availabilityZoneList.add(AvailabilityZone(availabilityZone))
        }
        return availabilityZoneList
    }

    private fun readVmTypes(): Map<AvailabilityZone, List<VmType>> {
        val availabilityZoneListHashMap = HashMap<AvailabilityZone, List<VmType>>()
        val availabilityZoneList = ArrayList<AvailabilityZone>()
        availabilityZoneList.addAll(getAvailabilityZones(USA_AVAILABILITY_ZONES))
        availabilityZoneList.addAll(getAvailabilityZones(EUROPE_AVAILABILITY_ZONES))

        val vmTypeList = ArrayList<VmType>()
        for (vmType in MockedVmTypes.values()) {
            vmTypeList.add(VmType.vmTypeWithMeta(vmType.value, vmType.vmTypeMeta))
        }

        for (availabilityZone in availabilityZoneList) {
            availabilityZoneListHashMap.put(availabilityZone, vmTypeList)
        }
        return availabilityZoneListHashMap
    }

    override fun scriptParams(): ScriptParams {
        return SCRIPT_PARAMS
    }

    override fun diskTypes(): DiskTypes {
        val diskMappings = HashMap<String, VolumeParameterType>()
        diskMappings.put(MockDiskType.MAGNETIC_DISK.value(), VolumeParameterType.MAGNETIC)
        diskMappings.put(MockDiskType.SSD.value(), VolumeParameterType.SSD)
        diskMappings.put(MockDiskType.EPHEMERAL.value(), VolumeParameterType.EPHEMERAL)
        return DiskTypes(diskTypes, defaultDiskType, diskMappings)
    }

    private val diskTypes: Collection<DiskType>
        get() {
            val disks = Lists.newArrayList<DiskType>()
            for (diskType in MockDiskType.values()) {
                disks.add(Companion.diskType(diskType.value))
            }
            return disks
        }

    private val defaultDiskType: DiskType
        get() = Companion.diskType(MockDiskType.MAGNETIC_DISK.value())

    override fun regions(): Regions {
        return Regions(regions.keys, defaultRegion)
    }

    override fun vmTypes(): VmTypes {
        val lists = LinkedHashSet<VmType>()
        for (vmTypeList in vmTypes.values) {
            lists.addAll(vmTypeList)
        }
        return VmTypes(lists, defaultVmType)
    }

    override fun availabilityZones(): AvailabilityZones {
        return AvailabilityZones(regions)
    }

    override fun resourceDefinition(resource: String): String {
        return MOCK_RESOURCE_DEFINITION
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        return ArrayList()
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList(Companion.orchestrator(OrchestratorConstants.SALT), Companion.orchestrator(OrchestratorConstants.SWARM)),
                Companion.orchestrator(OrchestratorConstants.SWARM))
    }

    private enum class MockDiskType private constructor(private val value: String) {
        MAGNETIC_DISK("magnetic"),
        SSD("ssd"),
        EPHEMERAL("ephemeral");

        fun value(): String {
            return value
        }
    }

    companion object {

        private val START_LABEL = 1
        private val SCRIPT_PARAMS = ScriptParams("mockdisk", START_LABEL)
        private val MOCK_RESOURCE_DEFINITION = "{}"
        private val EUROPE_AVAILABILITY_ZONES = arrayOf("europe-a", "europe-b")
        private val USA_AVAILABILITY_ZONES = arrayOf("usa-a", "usa-b", "usa-c")
    }
}
