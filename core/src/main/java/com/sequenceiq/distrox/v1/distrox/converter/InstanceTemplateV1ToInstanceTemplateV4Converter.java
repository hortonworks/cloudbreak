package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

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
        response.setRootVolume(ifNotNullF(source.getRootVolume(), volumeConverter::convert));
        response.setRootVolume(ifNotNullF(source.getRootVolume(), volumeConverter::convert));
        response.setAttachedVolumes(ifNotNullF(source.getAttachedVolumes(), volumeConverter::convert));
        response.setEphemeralVolume(ifNotNullF(source.getEphemeralVolume(), volumeConverter::convert));
        response.setAws(ifNotNullF(source.getAws(), instanceTemplateParameterConverter::convert));
        response.setAzure(ifNotNullF(source.getAzure(), instanceTemplateParameterConverter::convert));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setInstanceType(source.getInstanceType());
        return response;
    }
}
