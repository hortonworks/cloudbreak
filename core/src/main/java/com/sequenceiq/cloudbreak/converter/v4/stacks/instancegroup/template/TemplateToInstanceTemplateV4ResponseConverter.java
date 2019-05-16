package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.RootVolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class TemplateToInstanceTemplateV4ResponseConverter extends AbstractConversionServiceAwareConverter<Template, InstanceTemplateV4Response> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceTemplateV4Response convert(Template source) {
        InstanceTemplateV4Response response = new InstanceTemplateV4Response();
        response.setId(source.getId());
        response.setRootVolume(rootVolume(source));
        response.setAttachedVolumes(source.getVolumeTemplates().stream()
                .map(volume -> getConversionService().convert(volume, VolumeV4Response.class))
                .collect(Collectors.toSet()));
        response.setInstanceType(source.getInstanceType());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            Map<String, Object> parameters = attributes.getMap();
            ofNullable(source.getSecretAttributes()).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
            providerParameterCalculator.parse(parameters, response);
        }
        response.setCloudPlatform(CloudPlatform.valueOf(source.cloudPlatform()));
        return response;
    }

    private RootVolumeV4Response rootVolume(Template source) {
        RootVolumeV4Response response = new RootVolumeV4Response();
        response.setSize(source.getRootVolumeSize());
        return response;
    }
}
