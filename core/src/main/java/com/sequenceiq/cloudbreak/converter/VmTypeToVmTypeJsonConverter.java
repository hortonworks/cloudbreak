package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeMetaJson;
import com.sequenceiq.cloudbreak.api.model.VolumeParameterConfigJson;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;

@Component
public class VmTypeToVmTypeJsonConverter
        extends AbstractConversionServiceAwareConverter<VmType, VmTypeJson> {

    @Override
    public VmTypeJson convert(VmType source) {
        VmTypeJson vmTypeJson = new VmTypeJson();
        if (source != null) {
            List<VolumeParameterConfigJson> configs = new ArrayList<>();
            convertVolumeConfig(configs, source.getMetaData().getAutoAttachedConfig());
            convertVolumeConfig(configs, source.getMetaData().getEphemeralConfig());
            convertVolumeConfig(configs, source.getMetaData().getMagneticConfig());
            convertVolumeConfig(configs, source.getMetaData().getSsdConfig());
            convertVolumeConfig(configs, source.getMetaData().getSt1Config());

            VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
            vmTypeMetaJson.setProperties(source.getMetaData().getProperties());
            vmTypeMetaJson.setConfigs(configs);

            vmTypeJson.setVmTypeMetaJson(vmTypeMetaJson);
            vmTypeJson.setValue(source.value());
        }
        return vmTypeJson;
    }

    private void convertVolumeConfig(Collection<VolumeParameterConfigJson> configs, VolumeParameterConfig source) {
        if (source != null) {
            VolumeParameterConfigJson config = new VolumeParameterConfigJson();
            config.setMaximumNumber(source.maximumNumber());
            config.setMaximumSize(source.maximumSize());
            config.setMinimumNumber(source.minimumNumber());
            config.setMinimumSize(source.minimumSize());
            config.setVolumeParameterType(source.volumeParameterType().name());
            configs.add(config);
        }
    }
}
