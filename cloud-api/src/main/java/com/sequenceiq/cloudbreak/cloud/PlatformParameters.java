package com.sequenceiq.cloudbreak.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.util.JsonUtil;

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

    default Map<Region, List<AvailabilityZone>> readRegions(String zone) {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        try {
            RegionsSpecification oRegions = JsonUtil.readValue(zone, RegionsSpecification.class);
            for (RegionSpecification regionSpecification : oRegions.getItems()) {
                List<AvailabilityZone> av = new ArrayList<>();
                for (String s : regionSpecification.getZones()) {
                    av.add(AvailabilityZone.availabilityZone(s));
                }
                Collections.sort(av, new StringTypesCompare());
                regions.put(Region.region(regionSpecification.getName()), av);
            }
        } catch (IOException e) {
            return regions;
        }
        return sortMap(regions);
    }

}
