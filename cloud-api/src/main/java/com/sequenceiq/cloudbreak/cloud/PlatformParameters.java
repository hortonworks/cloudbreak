package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

/**
 * Platform parameters.
 */
public interface PlatformParameters {

    /**
     * Parameters for script generation
     *
     * @return the {@link ScriptParams} of a platform
     */
    ScriptParams scriptParams();

    /**
     * DiskTypes of a platform
     *
     * @return the {@link DiskTypes} of a platform
     */
    DiskTypes diskTypes();

    /**
     * Regions of a platform
     *
     * @return the {@link Regions} of a platform
     */
    Regions regions();

    /**
     * Virtual machine types of a platform
     *
     * @return the {@link VmTypes} of a platform
     */
    VmTypes vmTypes(Boolean extended);

    /**
     * Return the availability zones of a platform
     *
     * @return the {@link AvailabilityZones} of a platform
     */
    AvailabilityZones availabilityZones();

    /**
     * Return the definition of a resource in JSON format.
     *
     * @param resource type of the resource (available ones: "credential")
     * @return the definition in JSON
     */
    String resourceDefinition(String resource);

    /**
     * Return the additional stack parameters
     *
     * @return the {@link StackParamValidation} of a platform
     */
    List<StackParamValidation> additionalStackParameters();

    /**
     * Return the supported orchestrator types for a platform
     *
     * @return the {@link PlatformOrchestrator} of a platform
     */
    PlatformOrchestrator orchestratorParams();

    default <S extends StringType, O> Map<S, O> sortMap(Map<S, O> unsortMap) {
        Map<S, O> treeMap = new TreeMap<>(
                new Comparator<S>() {
                    @Override
                    public int compare(S o1, S o2) {
                        return o2.value().compareTo(o1.value());
                    }

                });
        treeMap.putAll(unsortMap);
        return treeMap;
    }

    default <T extends StringType> T nthElement(Collection<T> data, int n) {
        return data.stream().skip(n).findFirst().orElse(null);
    }

}
