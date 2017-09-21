package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;

@Component
public class PlatformRecommendationToPlatformRecommendationResponseConverter
        extends AbstractConversionServiceAwareConverter<PlatformRecommendation, RecommendationResponse> {

    @Override
    public RecommendationResponse convert(PlatformRecommendation source) {
        Map<String, VmTypeJson> result = new HashMap<>();
        source.getRecommendations().forEach((hostGroupName, vm) -> result.put(hostGroupName, getConversionService().convert(vm, VmTypeJson.class)));

        Set<VmTypeJson> vmTypes = source.getVirtualMachines()
                .stream().map(vmType -> getConversionService().convert(vmType, VmTypeJson.class)).collect(Collectors.toSet());

        return new RecommendationResponse(result, vmTypes);
    }
}
