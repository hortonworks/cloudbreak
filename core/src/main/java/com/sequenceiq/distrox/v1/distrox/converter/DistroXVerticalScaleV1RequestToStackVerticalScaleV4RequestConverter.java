package com.sequenceiq.distrox.v1.distrox.converter;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXVerticalScaleV1Request;

@Component
public class DistroXVerticalScaleV1RequestToStackVerticalScaleV4RequestConverter {

    @Inject
    private InstanceTemplateV1ToInstanceTemplateV4Converter instanceTemplateV1ToInstanceTemplateV4Converter;

    public StackVerticalScaleV4Request convert(DistroXVerticalScaleV1Request source, boolean gatewayType) {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();

        InstanceTemplateV4Request instanceTemplateV4Request = instanceTemplateV1ToInstanceTemplateV4Converter
                .convert(source.getTemplate(), null, gatewayType);

        stackVerticalScaleV4Request.setTemplate(instanceTemplateV4Request);
        stackVerticalScaleV4Request.setGroup(source.getGroup());
        stackVerticalScaleV4Request.setOrchestratorType(source.getOrchestratorType());
        return stackVerticalScaleV4Request;
    }

}
