package com.sequenceiq.cloudbreak.cloud.byos

import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator

import java.util.Arrays
import java.util.Collections
import java.util.HashMap

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.Orchestrator
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType

@Service
class BYOSPlatformParameters : PlatformParameters {

    override fun scriptParams(): ScriptParams {
        return ScriptParams("", 0)
    }

    override fun diskTypes(): DiskTypes {
        return DiskTypes(emptyList<DiskType>(), DiskType.diskType(""), diskMappings())
    }

    private fun diskMappings(): Map<String, VolumeParameterType> {
        return HashMap()
    }

    override fun regions(): Regions {
        return Regions(emptyList<Region>(), Region.region(""))
    }

    override fun vmTypes(): VmTypes {
        return VmTypes(emptyList<VmType>(), VmType.vmType(""))
    }

    override fun availabilityZones(): AvailabilityZones {
        return AvailabilityZones(emptyMap<Region, List<AvailabilityZone>>())
    }

    override fun resourceDefinition(resource: String): String {
        return ""
    }

    override fun additionalStackParameters(): List<StackParamValidation> {
        return emptyList<StackParamValidation>()
    }

    override fun orchestratorParams(): PlatformOrchestrator {
        return PlatformOrchestrator(Arrays.asList<Orchestrator>(), Companion.orchestrator(""))
    }
}
