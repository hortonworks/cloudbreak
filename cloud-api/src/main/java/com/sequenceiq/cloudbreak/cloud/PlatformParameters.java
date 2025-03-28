package com.sequenceiq.cloudbreak.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.model.Architecture;

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
     * Return the definition of a resource in JSON format.
     *
     * @param resource type of the resource (available ones: "credential")
     * @return the definition in JSON
     */
    String resourceDefinition(String resource);

    /**
     * Return the definition of a resource in JSON format if it lies in a deeper folder hierarchy.
     *
     * @param subDir the path for the actual directiory that contains the resource JSON. The path should start with '/'
     * @param resource type of the resource (available ones: "credential")
     * @return the definition in JSON
     */
    String resourceDefinitionInSubDir(String subDir, String resource);

    /**
     * Return the additional stack parameters
     *
     * @return the {@link StackParamValidation} of a platform
     */
    List<StackParamValidation> additionalStackParameters();

    /**
     * Returns the Set of instances types which are available for datahub
     *
     * @param architecture the specific architecture for which distrox enabled instances are required
     * @return Set of datahub enabled instances
     */
    default Set<String> getDistroxEnabledInstanceTypes(Architecture architecture) {
        return Collections.emptySet();
    }

    /**
     * Return the supported orchestrator types for a platform
     *
     * @return the {@link PlatformOrchestrator} of a platform
     */
    PlatformOrchestrator orchestratorParams();

    /**
     * Return the platform specific tag specification
     *
     * @return the {@link TagSpecification} of a platform
     */
    TagSpecification tagSpecification();

    /**
     * Return the platform specific tag validator
     *
     * @return the {@link TagValidator} of a platform
     */
    TagValidator tagValidator();

    /**
     * Return the platform specific image filter or an empty Optional is there is none
     *
     * @return the {@link ImageFilter} of a platform
     */
    default Optional<ImageFilter> imageFilter() {
        return Optional.empty();
    }

    /**
     * Default DiskType of a platform
     *
     * @return the {@link DiskType} of a platform
     */
    default DiskType defaultRootDiskType() {
        return null;
    }

    /**
     * The recommended virtual machine types for the platform
     *
     * @return the {@link VmRecommendations} of a platform
     */
    VmRecommendations recommendedVms();

    String platforName();

    boolean isAutoTlsSupported();

    default SpecialParameters specialParameters() {
        Map<String, Boolean> specialParameters = Maps.newHashMap();
        specialParameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE, Boolean.FALSE);
        specialParameters.put(PlatformParametersConsts.NETWORK_IS_MANDATORY, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.UPSCALING_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.DOWNSCALING_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.STARTSTOP_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.REGIONS_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.VOLUME_ATTACHMENT_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.VERTICAL_SCALING_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.DB_SUBNETS_UPDATE_ENABLED, Boolean.FALSE);
        specialParameters.put(PlatformParametersConsts.DELETE_VOLUMES_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.DISK_TYPE_CHANGE_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED, Boolean.TRUE);
        specialParameters.put(PlatformParametersConsts.DELAY_DATABASE_START, Boolean.FALSE);
        return new SpecialParameters(specialParameters);
    }

    default Map<String, InstanceGroupParameterResponse> collectInstanceGroupParameters(Set<InstanceGroupParameterRequest> instanceGroupParameterRequest) {
        return new HashMap<>();
    }

    default <S extends StringType, O> Map<S, O> sortMap(Map<S, O> unsortMap) {
        Map<S, O> treeMap = new TreeMap<>((o1, o2) -> o2.value().compareTo(o1.value()));
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
                av.sort(new StringTypesCompare());
                regions.put(Region.region(regionSpecification.getName()), av);
            }
        } catch (IOException ignored) {
            return regions;
        }
        return sortMap(regions);
    }
}
