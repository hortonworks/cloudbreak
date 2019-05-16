package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;

@Component
public class TemplateToInstanceTemplateV4RequestConverter extends AbstractConversionServiceAwareConverter<Template, InstanceTemplateV4Request> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceTemplateV4Request convert(Template source) {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        Map<String, Object> parameters = new HashMap<>();
        ofNullable(source.getAttributes()).ifPresent(attr -> parameters.putAll(attr.getMap()));
        ofNullable(source.getSecretAttributes()).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
        providerParameterCalculator.parse(parameters, templateRequest);
        templateRequest.setInstanceType(source.getInstanceType());
        templateRequest.setAttachedVolumes(source.getVolumeTemplates().stream().map(this::getAttachedVolume).collect(Collectors.toSet()));
        templateRequest.setRootVolume(getRootVolume(source));
        return templateRequest;
    }

    private RootVolumeV4Request getRootVolume(Template source) {
        RootVolumeV4Request ret = new RootVolumeV4Request();
        ret.setSize(source.getRootVolumeSize());
        return ret;
    }

    private VolumeV4Request getAttachedVolume(VolumeTemplate source) {
        VolumeV4Request ret = new VolumeV4Request();
        ret.setCount(source.getVolumeCount());
        ret.setSize(source.getVolumeSize());
        ret.setType(source.getVolumeType());
        return ret;
    }
}
