package com.sequenceiq.cloudbreak.controller

import java.util.ArrayList
import java.util.HashMap

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint
import com.sequenceiq.cloudbreak.api.model.JsonEntity
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson
import com.sequenceiq.cloudbreak.api.model.VmTypeJson
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService

@Component
class PlatformParameterController : ConnectorEndpoint {

    @Autowired
    private val cloudParameterService: CloudParameterService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun getPlatforms(): Map<String, JsonEntity> {
        val pv = cloudParameterService!!.platformVariants
        val diskTypes = cloudParameterService.diskTypes
        val vmtypes = cloudParameterService.vmtypes
        val regions = cloudParameterService.regions
        val orchestrators = cloudParameterService.orchestrators

        val map = HashMap<String, JsonEntity>()

        map.put("variants", conversionService!!.convert<PlatformVariantsJson>(pv, PlatformVariantsJson::class.java))
        map.put("disks", conversionService.convert<PlatformDisksJson>(diskTypes, PlatformDisksJson::class.java))
        map.put("virtualMachines", conversionService.convert<PlatformVirtualMachinesJson>(vmtypes, PlatformVirtualMachinesJson::class.java))
        map.put("regions", conversionService.convert<PlatformRegionsJson>(regions, PlatformRegionsJson::class.java))
        map.put("orchestrators", conversionService.convert<PlatformOrchestratorsJson>(orchestrators, PlatformOrchestratorsJson::class.java))

        return map
    }

    override fun getPlatformVariants(): PlatformVariantsJson {
        val pv = cloudParameterService!!.platformVariants
        return conversionService!!.convert<PlatformVariantsJson>(pv, PlatformVariantsJson::class.java)
    }

    override fun getPlatformVariantByType(type: String): Collection<String> {
        val pv = cloudParameterService!!.platformVariants
        val strings = conversionService!!.convert<PlatformVariantsJson>(pv, PlatformVariantsJson::class.java).platformToVariants[type.toUpperCase()]
        return strings ?: ArrayList<String>()
    }

    override fun getDisktypes(): PlatformDisksJson {
        val dts = cloudParameterService!!.diskTypes
        return conversionService!!.convert<PlatformDisksJson>(dts, PlatformDisksJson::class.java)
    }

    override fun getDisktypeByType(type: String): Collection<String> {
        val diskTypes = cloudParameterService!!.diskTypes
        val strings = conversionService!!.convert<PlatformDisksJson>(diskTypes, PlatformDisksJson::class.java).diskTypes[type.toUpperCase()]
        return strings ?: ArrayList<String>()
    }

    override fun getOrchestratortypes(): PlatformOrchestratorsJson {
        val orchestrators = cloudParameterService!!.orchestrators
        return conversionService!!.convert<PlatformOrchestratorsJson>(orchestrators, PlatformOrchestratorsJson::class.java)
    }

    override fun getOchestratorsByType(type: String): Collection<String> {
        val orchestrators = cloudParameterService!!.orchestrators
        val strings = conversionService!!.convert<PlatformOrchestratorsJson>(orchestrators, PlatformOrchestratorsJson::class.java).orchestrators[type.toUpperCase()]
        return strings ?: ArrayList<String>()
    }

    override fun getVmTypes(): PlatformVirtualMachinesJson {
        val vmtypes = cloudParameterService!!.vmtypes
        return conversionService!!.convert<PlatformVirtualMachinesJson>(vmtypes, PlatformVirtualMachinesJson::class.java)
    }

    override fun getVmTypeByType(type: String): Collection<VmTypeJson> {
        val vmtypes = cloudParameterService!!.vmtypes
        val vmTypes = conversionService!!.convert<PlatformVirtualMachinesJson>(vmtypes, PlatformVirtualMachinesJson::class.java).virtualMachines[type.toUpperCase()]
        return vmTypes ?: ArrayList<VmTypeJson>()
    }

    override fun getRegions(): PlatformRegionsJson {
        val pv = cloudParameterService!!.regions
        return conversionService!!.convert<PlatformRegionsJson>(pv, PlatformRegionsJson::class.java)
    }

    override fun getRegionRByType(type: String): Collection<String> {
        val pv = cloudParameterService!!.regions
        val regions = conversionService!!.convert<PlatformRegionsJson>(pv, PlatformRegionsJson::class.java).regions[type.toUpperCase()]
        return regions ?: ArrayList<String>()
    }

    override fun getRegionAvByType(type: String): Map<String, Collection<String>> {
        val pv = cloudParameterService!!.regions
        val azs = conversionService!!.convert<PlatformRegionsJson>(pv, PlatformRegionsJson::class.java).availabilityZones[type.toUpperCase()]
        return azs ?: HashMap<String, Collection<String>>()
    }

}
