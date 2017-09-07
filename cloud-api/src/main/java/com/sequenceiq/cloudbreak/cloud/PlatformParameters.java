package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
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
     * Virtual machine types of a platform in availability zones.
     *
     * @return the {@link AvailabilityZone}, {@link VmTypes} map of a platform
     */
    Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended);

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

    /**
     * Return the supported images types for a platform
     *
     * @return the {@link PlatformImage} of a platform
     */
    PlatformImage images();

    /**
     * Return the supported images regex for a platform
     *
     * @return the {@link String} of a platform
     */
    String imageRegex();

    /**
     * Return the platform specific tag specification
     *
     * @return the {@link TagSpecification} of a platform
     */
    TagSpecification tagSpecification();

    String getDefaultRegionsConfigString();

    String getDefaultRegionString();

    String platforName();

    default <S extends StringType, O> Map<S, O> sortMap(Map<S, O> unsortMap) {
        Map<S, O> treeMap = new TreeMap<>((o1, o2) -> o2.value().compareTo(o1.value()));
        treeMap.putAll(unsortMap);
        return treeMap;
    }

    default Region getRegionByName(String name) {
        for (Region region : regions().types()) {
            if (name.equals(region.value())) {
                return region;
            }
        }
        return null;
    }

    default Region getDefaultRegion() {
        Map<Platform, Region> regions = Maps.newHashMap();
        if (isNoneEmpty(getDefaultRegionsConfigString())) {
            for (String entry : getDefaultRegionsConfigString().split(",")) {
                String[] keyValue = entry.split(":");
                regions.put(platform(keyValue[0]), Region.region(keyValue[1]));
            }
            Region platformRegion = regions.get(platforName());
            if (platformRegion != null && !isEmpty(platformRegion.value())) {
                return getRegionByName(platformRegion.value());
            }
        }
        return getRegionByName(getDefaultRegionString());
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
                av.sort(new StringTypesCompare());
                regions.put(Region.region(regionSpecification.getName()), av);
            }
        } catch (IOException e) {
            return regions;
        }
        return sortMap(regions);
    }

}
