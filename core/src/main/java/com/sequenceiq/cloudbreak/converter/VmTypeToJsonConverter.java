package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeMetaJson;
import com.sequenceiq.cloudbreak.api.model.VolumeParameterConfigJson;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;

@Component
public class VmTypeToJsonConverter extends AbstractConversionServiceAwareConverter<VmType, VmTypeJson> {

    @Override
    public VmTypeJson convert(VmType item) {
        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(item.getMetaData().getProperties());
        VolumeParameterConfig autoAttachedConfig = item.getMetaData().getAutoAttachedConfig();
        if (autoAttachedConfig != null) {
            VolumeParameterConfigJson volumeParameterConfigJson = new VolumeParameterConfigJson();
            volumeParameterConfigJson.setVolumeParameterType(autoAttachedConfig.volumeParameterType().name());
            volumeParameterConfigJson.setMaximumNumber(autoAttachedConfig.maximumNumber());
            volumeParameterConfigJson.setMinimumNumber(autoAttachedConfig.minimumNumber());
            volumeParameterConfigJson.setMaximumSize(autoAttachedConfig.maximumSize());
            volumeParameterConfigJson.setMinimumSize(autoAttachedConfig.minimumSize());
            vmTypeMetaJson.getConfigs().add(volumeParameterConfigJson);
        }
        VolumeParameterConfig ephemeralConfig = item.getMetaData().getEphemeralConfig();
        if (ephemeralConfig != null) {
            VolumeParameterConfigJson volumeParameterConfigJson = new VolumeParameterConfigJson();
            volumeParameterConfigJson.setVolumeParameterType(ephemeralConfig.volumeParameterType().name());
            volumeParameterConfigJson.setMaximumNumber(ephemeralConfig.maximumNumber());
            volumeParameterConfigJson.setMinimumNumber(ephemeralConfig.minimumNumber());
            volumeParameterConfigJson.setMaximumSize(ephemeralConfig.maximumSize());
            volumeParameterConfigJson.setMinimumSize(ephemeralConfig.minimumSize());
            vmTypeMetaJson.getConfigs().add(volumeParameterConfigJson);
        }
        VolumeParameterConfig magneticConfig = item.getMetaData().getMagneticConfig();
        if (magneticConfig != null) {
            VolumeParameterConfigJson volumeParameterConfigJson = new VolumeParameterConfigJson();
            volumeParameterConfigJson.setVolumeParameterType(magneticConfig.volumeParameterType().name());
            volumeParameterConfigJson.setMaximumNumber(magneticConfig.maximumNumber());
            volumeParameterConfigJson.setMinimumNumber(magneticConfig.minimumNumber());
            volumeParameterConfigJson.setMaximumSize(magneticConfig.maximumSize());
            volumeParameterConfigJson.setMinimumSize(magneticConfig.minimumSize());
            vmTypeMetaJson.getConfigs().add(volumeParameterConfigJson);
        }
        VolumeParameterConfig ssdConfig = item.getMetaData().getSsdConfig();
        if (ssdConfig != null) {
            VolumeParameterConfigJson volumeParameterConfigJson = new VolumeParameterConfigJson();
            volumeParameterConfigJson.setVolumeParameterType(ssdConfig.volumeParameterType().name());
            volumeParameterConfigJson.setMaximumNumber(ssdConfig.maximumNumber());
            volumeParameterConfigJson.setMinimumNumber(ssdConfig.minimumNumber());
            volumeParameterConfigJson.setMaximumSize(ssdConfig.maximumSize());
            volumeParameterConfigJson.setMinimumSize(ssdConfig.minimumSize());
            vmTypeMetaJson.getConfigs().add(volumeParameterConfigJson);
        }
        VolumeParameterConfig st1Config = item.getMetaData().getSt1Config();
        if (st1Config != null) {
            VolumeParameterConfigJson volumeParameterConfigJson = new VolumeParameterConfigJson();
            volumeParameterConfigJson.setVolumeParameterType(st1Config.volumeParameterType().name());
            volumeParameterConfigJson.setMaximumNumber(st1Config.maximumNumber());
            volumeParameterConfigJson.setMinimumNumber(st1Config.minimumNumber());
            volumeParameterConfigJson.setMaximumSize(st1Config.maximumSize());
            volumeParameterConfigJson.setMinimumSize(st1Config.minimumSize());
            vmTypeMetaJson.getConfigs().add(volumeParameterConfigJson);
        }
        return new VmTypeJson(item.value(), vmTypeMetaJson);
    }
}
