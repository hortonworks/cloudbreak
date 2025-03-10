package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.GcpInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class InstanceTemplateV1ToInstanceTemplateV4Converter {

    @Inject
    private VolumeConverter volumeConverter;

    @Inject
    private InstanceTemplateParameterConverter instanceTemplateParameterConverter;

    public InstanceTemplateV4Request convert(InstanceTemplateV1Request source, DetailedEnvironmentResponse environment, boolean gatewayType) {
        InstanceTemplateV4Request response = new InstanceTemplateV4Request();
        response.setRootVolume(getIfNotNull(source.getRootVolume(),
                rootVolumeV1Request -> volumeConverter.convert(rootVolumeV1Request, environment.getCloudPlatform(), gatewayType)));
        response.setAttachedVolumes(getIfNotNull(source.getAttachedVolumes(), volumeConverter::convertTo));
        response.setEphemeralVolume(getIfNotNull(source.getEphemeralVolume(), volumeConverter::convert));
        AwsInstanceTemplateV1Parameters awsParametersEffective = Objects.requireNonNullElse(source.getAws(),
                new AwsInstanceTemplateV1Parameters());
        response.setAws(instanceTemplateParameterConverter.convert(awsParametersEffective, environment));
        AzureInstanceTemplateV1Parameters azureParametersEffective = Objects.requireNonNullElse(source.getAzure(),
                new AzureInstanceTemplateV1Parameters());
        response.setAzure(instanceTemplateParameterConverter.convert(azureParametersEffective, environment));
        GcpInstanceTemplateV1Parameters gcpParametersEffective = Objects.requireNonNullElse(source.getGcp(),
                new GcpInstanceTemplateV1Parameters());
        response.setGcp(instanceTemplateParameterConverter.convert(gcpParametersEffective, environment));
        response.setYarn(getIfNotNull(source.getYarn(), instanceTemplateParameterConverter::convert));
        response.setCloudPlatform(source.getCloudPlatform());
        response.setInstanceType(source.getInstanceType());
        response.setTemporaryStorage(source.getTemporaryStorage());
        return response;
    }

    public InstanceTemplateV1Request convert(InstanceTemplateV4Request source, DetailedEnvironmentResponse environment, boolean gatewayType) {
        InstanceTemplateV1Request response = new InstanceTemplateV1Request();
        response.setRootVolume(getIfNotNull(source.getRootVolume(), rootVolumeV4Request ->
                volumeConverter.convert(rootVolumeV4Request, environment.getCloudPlatform(), gatewayType)));
        response.setAttachedVolumes(getIfNotNull(source.getAttachedVolumes(), volumeConverter::convertFrom));
        response.setEphemeralVolume(getIfNotNull(source.getEphemeralVolume(), volumeConverter::convert));
        response.setAws(getIfNotNull(source.getAws(), environment, instanceTemplateParameterConverter::convert));
        response.setAzure(getIfNotNull(source.getAzure(), instanceTemplateParameterConverter::convert));
        response.setGcp(getIfNotNull(source.getGcp(), instanceTemplateParameterConverter::convert));
        response.setYarn(getIfNotNull(source.getYarn(), instanceTemplateParameterConverter::convert));
        response.setInstanceType(source.getInstanceType());
        return response;
    }

}
