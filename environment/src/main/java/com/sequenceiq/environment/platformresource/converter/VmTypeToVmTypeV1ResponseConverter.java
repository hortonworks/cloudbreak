package com.sequenceiq.environment.platformresource.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.VmTypeMetaJson;
import com.sequenceiq.environment.api.platformresource.model.VmTypeV1Response;
import com.sequenceiq.environment.api.platformresource.model.VolumeParameterConfigV1Response;

@Component
public class VmTypeToVmTypeV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<VmType, VmTypeV1Response> {

    @Override
    public VmTypeV1Response convert(VmType source) {
        VmTypeV1Response vmTypeV1Response = new VmTypeV1Response();
        List<VolumeParameterConfigV1Response> configs = new ArrayList<>();
        convertVolumeConfig(configs, source.getMetaData().getAutoAttachedConfig());
        convertVolumeConfig(configs, source.getMetaData().getEphemeralConfig());
        convertVolumeConfig(configs, source.getMetaData().getMagneticConfig());
        convertVolumeConfig(configs, source.getMetaData().getSsdConfig());
        convertVolumeConfig(configs, source.getMetaData().getSt1Config());

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(source.getMetaData().getProperties());
        vmTypeMetaJson.setConfigs(configs);

        vmTypeV1Response.setVmTypeMetaJson(vmTypeMetaJson);
        vmTypeV1Response.setValue(source.value());
        return vmTypeV1Response;
    }

    private void convertVolumeConfig(Collection<VolumeParameterConfigV1Response> configs, VolumeParameterConfig source) {
        if (source != null) {
            VolumeParameterConfigV1Response config = new VolumeParameterConfigV1Response();
            config.setMaximumNumber(source.maximumNumber());
            config.setMaximumSize(source.maximumSize());
            config.setMinimumNumber(source.minimumNumber());
            config.setMinimumSize(source.minimumSize());
            config.setVolumeParameterType(source.volumeParameterType().name());
            configs.add(config);
        }
    }
}
