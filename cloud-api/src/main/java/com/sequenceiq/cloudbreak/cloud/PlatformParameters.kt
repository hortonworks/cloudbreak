package com.sequenceiq.cloudbreak.cloud

import java.util.Comparator
import java.util.TreeMap

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

/**
 * Platform parameters.
 */
interface PlatformParameters {

    /**
     * Parameters for script generation

     * @return the [ScriptParams] of a platform
     */
    fun scriptParams(): ScriptParams

    /**
     * DiskTypes of a platform

     * @return the [DiskTypes] of a platform
     */
    fun diskTypes(): DiskTypes

    /**
     * Regions of a platform

     * @return the [Regions] of a platform
     */
    fun regions(): Regions

    /**
     * Virtual machine types of a platform

     * @return the [VmTypes] of a platform
     */
    fun vmTypes(): VmTypes

    /**
     * Return the availability zones of a platform

     * @return the [AvailabilityZones] of a platform
     */
    fun availabilityZones(): AvailabilityZones

    /**
     * Return the definition of a resource in JSON format.

     * @param resource type of the resource (available ones: "credential")
     * *
     * @return the definition in JSON
     */
    fun resourceDefinition(resource: String): String

    /**
     * Return the additional stack parameters

     * @return the [StackParamValidation] of a platform
     */
    fun additionalStackParameters(): List<StackParamValidation>

    /**
     * Return the supported orchestrator types for a platform

     * @return the [PlatformOrchestrator] of a platform
     */
    fun orchestratorParams(): PlatformOrchestrator

    fun <S : StringType, O : Any> sortMap(unsortMap: Map<S, O>): Map<S, O> {
        val treeMap = TreeMap<S, O>(
                Comparator<S> { o1, o2 -> o2.value().compareTo(o1.value()) })
        treeMap.putAll(unsortMap)
        return treeMap
    }

    fun <T : StringType> nthElement(data: Collection<T>, n: Int): T {
        return data.stream().skip(n.toLong()).findFirst().orElse(null)
    }

}
