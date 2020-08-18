package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;

@Component
public class InstanceTemplateV1ToInstanceTemplateV4Converter {

    @Inject
    private VolumeConverter volumeConverter;

    @Inject
    private InstanceTemplateParameterConverter instanceTemplateParameterConverter;

    public InstanceTemplateV4Request convert(InstanceTemplateV1Request source) {
        InstanceTemplateV4Request response = new InstanceTemplateV4Request();
        response.setRootVolume(getIfNotNull(source.getRootVolume(), volumeConverter::convert));
        response.setRootVolume(getIfNotNull(source.getRootVolume(), volumeConverter::convert));
        response.setAttachedVolumes(getIfNotNull(source.getAttachedVolumes(), volumeConverter::convertTo));
        response.setEphemeralVolume(getIfNotNull(source.getEphemeralVolume(), volumeConverter::convert));
        response.setAws(getIfNotNull(source.getAws(), instanceTemplateParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceTemplateParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceTemplateParameterConverter::convert));
        response.setOpenstack(getIfNotNull(source.getOpenstack(), instanceTemplateParameterConverter::convert));
        response.setYarn(getIfNotNull(source.getYarn(), instanceTemplateParameterConverter::convert));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setInstanceType(source.getInstanceType());
        return response;
    }

    public InstanceTemplateV1Request convert(InstanceTemplateV4Request source) {
        InstanceTemplateV1Request response = new InstanceTemplateV1Request();
        response.setRootVolume(getIfNotNull(source.getRootVolume(), volumeConverter::convert));
        response.setRootVolume(getIfNotNull(source.getRootVolume(), volumeConverter::convert));
        response.setAttachedVolumes(getIfNotNull(source.getAttachedVolumes(), volumeConverter::convertFrom));
        response.setEphemeralVolume(getIfNotNull(source.getEphemeralVolume(), volumeConverter::convert));
        response.setAws(getIfNotNull(source.getAws(), instanceTemplateParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceTemplateParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceTemplateParameterConverter::convert));
        response.setOpenstack(getIfNotNull(source.getOpenstack(), instanceTemplateParameterConverter::convert));
        response.setYarn(getIfNotNull(source.getYarn(), instanceTemplateParameterConverter::convert));
        response.setInstanceType(source.getInstanceType());
        return response;
    }
}
