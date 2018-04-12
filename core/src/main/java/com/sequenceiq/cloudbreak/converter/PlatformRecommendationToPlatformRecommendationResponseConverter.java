package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.DiskResponse;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PlatformRecommendationToPlatformRecommendationResponseConverter
        extends AbstractConversionServiceAwareConverter<PlatformRecommendation, RecommendationResponse> {

    @Override
    public RecommendationResponse convert(PlatformRecommendation source) {
        Map<String, VmTypeJson> result = new HashMap<>();
        source.getRecommendations().forEach((hostGroupName, vm) -> result.put(hostGroupName, getConversionService().convert(vm, VmTypeJson.class)));

        Set<VmTypeJson> vmTypes = source.getVirtualMachines()
                .stream().map(vmType -> getConversionService().convert(vmType, VmTypeJson.class)).collect(Collectors.toSet());

        Set<DiskResponse> diskResponses = new HashSet<>();
        for (Entry<DiskType, DisplayName> diskTypeDisplayName : source.getDiskTypes().displayNames().entrySet()) {
            for (Entry<String, VolumeParameterType> volumeParameterType : source.getDiskTypes().diskMapping().entrySet()) {
                if (diskTypeDisplayName.getKey().value().equals(volumeParameterType.getKey())) {
                    DiskResponse diskResponse = new DiskResponse(
                            diskTypeDisplayName.getKey().value(),
                            volumeParameterType.getValue().name(),
                            diskTypeDisplayName.getValue().value());
                    diskResponses.add(diskResponse);
                }

            }


        }

        return new RecommendationResponse(result, vmTypes, diskResponses);
    }
}
