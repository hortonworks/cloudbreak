package com.sequenceiq.cloudbreak.cloud.openstack.common

import com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region.region
import com.sequenceiq.cloudbreak.cloud.model.VmType.vmType

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import org.springframework.stereotype.Service

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
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants
import com.sequenceiq.cloudbreak.util.FileReaderUtils

@Service
class OpenStackParameters : PlatformParameters {

    override fun scriptParams(): ScriptParams {
        return SCRIPT_PARAMS
    }

    override fun diskTypes(): DiskTypes {
        return DiskTypes(diskTypes, defaultDiskType(), diskMappings())
    }

    private val diskTypes: Collection<DiskType>
        get() = ArrayList()

    private fun defaultDiskType(): DiskType {
        return Companion.diskType("HDD")
    }

    private fun diskMappings(): Map<String, VolumeParameterType> {
        val map = HashMap<String, VolumeParameterType>()
        map.put("HDD", VolumeParameterType.MAGNETIC)
        return map
    }

    override fun regions(): Regions {
        return Regions(regions, defaultRegion())
    }

    private val regions: Collection<Region>
        get() {
            val regions = ArrayList<Region>()
            regions.add(Companion.region("local"))
            return regions
        }

    private fun defaultRegion(): Region {
        return Companion.region("local")
    }

    override fun availabilityZones(): AvailabilityZones {
        val availabiltyZones = HashMap<Region, List<AvailabilityZone>>()
        availabiltyZones.put(Companion.region("local"), ArrayList<AvailabilityZone>())
        return AvailabilityZones(availabiltyZones)
    }

    override fun resourceDefinition(resource: String): String {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/openstack-$resource.json")
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        return emptyList<StackParamValidation>()
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList<T>(Companion.orchestrator(OrchestratorConstants.SALT)), Companion.orchestrator(OrchestratorConstants.SALT))
    }

    override fun vmTypes(): VmTypes {
        return VmTypes(virtualMachines(), defaultVirtualMachine())
    }

    private fun virtualMachines(): Collection<VmType> {
        return ArrayList()
    }

    private fun defaultVirtualMachine(): VmType {
        return Companion.vmType("")
    }

    companion object {
        private val START_LABEL = Integer.valueOf(97)
        private val SCRIPT_PARAMS = ScriptParams("vd", START_LABEL)
    }
}
