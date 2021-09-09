package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.DiskV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.GatewayRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.InstanceCountV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ResizeRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.v4.connectors.VmTypeToVmTypeV4ResponseConverter;

@Component
public class PlatformRecommendationToPlatformRecommendationV4ResponseConverter {

    @Inject
    private VmTypeToVmTypeV4ResponseConverter vmTypeToVmTypeV4ResponseConverter;

    public RecommendationV4Response convert(PlatformRecommendation source) {
        Map<String, VmTypeV4Response> result = new HashMap<>();
        source.getRecommendations().forEach((hostGroupName, vm) -> result.put(
                hostGroupName,
                vmTypeToVmTypeV4ResponseConverter.convert(vm)));

        Set<VmTypeV4Response> vmTypes = source.getVirtualMachines()
                .stream().map(vmType -> vmTypeToVmTypeV4ResponseConverter.convert(vmType)).collect(Collectors.toSet());

        Set<DiskV4Response> diskResponses = new HashSet<>();
        for (Entry<DiskType, DisplayName> diskTypeDisplayName : source.getDiskTypes().displayNames().entrySet()) {
            for (Entry<String, VolumeParameterType> volumeParameterType : source.getDiskTypes().diskMapping().entrySet()) {
                if (diskTypeDisplayName.getKey().value().equals(volumeParameterType.getKey())) {
                    DiskV4Response diskResponse = new DiskV4Response(
                            diskTypeDisplayName.getKey().value(),
                            volumeParameterType.getValue().name(),
                            diskTypeDisplayName.getValue().value());
                    diskResponses.add(diskResponse);
                }
            }
        }

        Map<String, InstanceCountV4Response> instanceCounts = new TreeMap<>();
        source.getInstanceCounts().forEach((hostGroupName, instanceCount) ->
                instanceCounts.put(hostGroupName, new InstanceCountV4Response(
                        instanceCount.getMinimumCount(),
                        instanceCount.getMaximumCount()
                ))
        );

        GatewayRecommendationV4Response gateway = new GatewayRecommendationV4Response(source.getGatewayRecommendation().getHostGroups());

        AutoscaleRecommendationV4Response autoscaleRecommendation = new AutoscaleRecommendationV4Response(
                source.getAutoscaleRecommendation().getTimeBasedHostGroups(), source.getAutoscaleRecommendation().getLoadBasedHostGroups());

        ResizeRecommendationV4Response resizeRecommendation = new ResizeRecommendationV4Response(source.getResizeRecommendation().getScaleUpHostGroups(),
                source.getResizeRecommendation().getScaleDownHostGroups());

        return new RecommendationV4Response(result, vmTypes, diskResponses, instanceCounts, gateway, autoscaleRecommendation, resizeRecommendation);
    }
}
