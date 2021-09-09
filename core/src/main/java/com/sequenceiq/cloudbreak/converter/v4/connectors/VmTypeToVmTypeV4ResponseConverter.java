package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeMetaJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VolumeParameterConfigV4Response;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;

@Component
public class VmTypeToVmTypeV4ResponseConverter {

    public VmTypeV4Response convert(VmType source) {
        VmTypeV4Response vmTypeV4Response = new VmTypeV4Response();
        List<VolumeParameterConfigV4Response> configs = new ArrayList<>();
        convertVolumeConfig(configs, source.getMetaData().getAutoAttachedConfig());
        convertVolumeConfig(configs, source.getMetaData().getEphemeralConfig());
        convertVolumeConfig(configs, source.getMetaData().getMagneticConfig());
        convertVolumeConfig(configs, source.getMetaData().getSsdConfig());
        convertVolumeConfig(configs, source.getMetaData().getSt1Config());

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(source.getMetaData().getProperties());
        vmTypeMetaJson.setConfigs(configs);

        vmTypeV4Response.setVmTypeMetaJson(vmTypeMetaJson);
        vmTypeV4Response.setValue(source.value());
        return vmTypeV4Response;
    }

    private void convertVolumeConfig(Collection<VolumeParameterConfigV4Response> configs, VolumeParameterConfig source) {
        if (source != null) {
            VolumeParameterConfigV4Response config = new VolumeParameterConfigV4Response();
            config.setMaximumNumber(source.maximumNumber());
            config.setMaximumSize(source.maximumSize());
            config.setMinimumNumber(source.minimumNumber());
            config.setMinimumSize(source.minimumSize());
            config.setVolumeParameterType(source.volumeParameterType().name());
            configs.add(config);
        }
    }
}
