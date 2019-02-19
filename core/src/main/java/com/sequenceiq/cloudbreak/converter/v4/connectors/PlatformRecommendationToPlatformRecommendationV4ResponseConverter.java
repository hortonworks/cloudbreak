package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.DiskV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class PlatformRecommendationToPlatformRecommendationV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<PlatformRecommendation, RecommendationV4Response> {

    @Override
    public RecommendationV4Response convert(PlatformRecommendation source) {
        Map<String, VmTypeV4Response> result = new HashMap<>();
        source.getRecommendations().forEach((hostGroupName, vm) -> result.put(hostGroupName, getConversionService().convert(vm, VmTypeV4Response.class)));

        Set<VmTypeV4Response> vmTypes = source.getVirtualMachines()
                .stream().map(vmType -> getConversionService().convert(vmType, VmTypeV4Response.class)).collect(Collectors.toSet());

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
        return new RecommendationV4Response(result, vmTypes, diskResponses);
    }
}
