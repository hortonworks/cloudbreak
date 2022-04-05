package com.sequenceiq.datalake.converter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;
import com.sequenceiq.sdx.api.model.VolumeParameterConfigResponse;

@Component
public class VmTypeConverter {

    public VmTypeResponse convert(com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse source) {
        return new VmTypeResponse(source.getValue(), convert(source.getVmTypeMetaJson()));
    }

    public List<VmTypeResponse> convert(Collection<com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        return source.stream().map(this::convert).collect(Collectors.toList());
    }

    private VmTypeMetaJson convert(com.sequenceiq.environment.api.v1.platformresource.model.VmTypeMetaJson source) {
        List<VolumeParameterConfigResponse> volumeParameterConfigs = source.getConfigs().stream()
                .map(this::convert).collect(Collectors.toList());
        return new VmTypeMetaJson(volumeParameterConfigs, source.getProperties());
    }

    private VolumeParameterConfigResponse convert(com.sequenceiq.environment.api.v1.platformresource.model.VolumeParameterConfigResponse source) {
        return new VolumeParameterConfigResponse(source.getVolumeParameterType(), source.getMinimumSize(), source.getMaximumSize(),
                source.getMinimumNumber(), source.getMaximumNumber());
    }
}
