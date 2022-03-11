package com.sequenceiq.freeipa.converter.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeMetaJson;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VolumeParameterConfigResponse;

@Component
public class VmTypeToVmTypeResponseConverter {

    public VmTypeResponse convert(VmType source) {
        VmTypeResponse vmTypeResponse = new VmTypeResponse();
        List<VolumeParameterConfigResponse> configs = new ArrayList<>();
        convertVolumeConfig(configs, source.getMetaData().getAutoAttachedConfig());
        convertVolumeConfig(configs, source.getMetaData().getEphemeralConfig());
        convertVolumeConfig(configs, source.getMetaData().getMagneticConfig());
        convertVolumeConfig(configs, source.getMetaData().getSsdConfig());
        convertVolumeConfig(configs, source.getMetaData().getSt1Config());

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(source.getMetaData().getProperties());
        vmTypeMetaJson.setConfigs(configs);

        vmTypeResponse.setVmTypeMetaJson(vmTypeMetaJson);
        vmTypeResponse.setValue(source.value());
        return vmTypeResponse;
    }

    private void convertVolumeConfig(Collection<VolumeParameterConfigResponse> configs, VolumeParameterConfig source) {
        if (source != null) {
            VolumeParameterConfigResponse config = new VolumeParameterConfigResponse();
            config.setMaximumNumber(source.maximumNumber());
            config.setMaximumSize(source.maximumSize());
            config.setMinimumNumber(source.minimumNumber());
            config.setMinimumSize(source.minimumSize());
            config.setVolumeParameterType(source.volumeParameterType().name());
            configs.add(config);
        }
    }
}