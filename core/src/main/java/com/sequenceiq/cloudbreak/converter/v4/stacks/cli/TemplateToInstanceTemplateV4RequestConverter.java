package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateToInstanceTemplateV4RequestConverter extends AbstractConversionServiceAwareConverter<Template, InstanceTemplateV4Request> {

    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceTemplateV4Request convert(Template source) {
        InstanceTemplateV4Request templateRequest = new InstanceTemplateV4Request();
        Map<String, Object> parameters = new HashMap<>();
        ofNullable(source.getAttributes()).ifPresent(attr -> parameters.putAll(attr.getMap()));
        ofNullable(source.getSecretAttributes()).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
        providerParameterCalculator.to(parameters, templateRequest);
        templateRequest.setInstanceType(source.getInstanceType());
        templateRequest.setAttachedVolumes(Set.of(getAttachedVolume(source)));
        templateRequest.setRootVolume(getRootVolume(source));
        return templateRequest;
    }

    private VolumeV4Request getRootVolume(Template source) {
        VolumeV4Request ret = new VolumeV4Request();
        ret.setCount(1);
        ret.setSize(source.getVolumeSize());
        return ret;
    }

    private VolumeV4Request getAttachedVolume(Template source) {
        VolumeV4Request ret = new VolumeV4Request();
        ret.setCount(source.getVolumeCount());
        ret.setSize(source.getVolumeSize());
        ret.setType(source.getVolumeType());
        return ret;
    }
}
