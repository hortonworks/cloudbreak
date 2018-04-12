package com.sequenceiq.cloudbreak.converter.v2;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class TemplateV2RequestToTemplateRequestConverter extends AbstractConversionServiceAwareConverter<TemplateV2Request, TemplateRequest> {
    @Inject
    private TopologyService topologyService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public TemplateRequest convert(TemplateV2Request source) {
        TemplateRequest template = new TemplateRequest();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setVolumeCount(source.getVolumeCount());
        template.setVolumeSize(source.getVolumeSize());
        template.setInstanceType(source.getInstanceType());
        String volumeType = source.getVolumeType();
        template.setVolumeType(volumeType == null ? "HDD" : volumeType);
        template.setCustomInstanceType(source.getCustomInstanceType());
        template.setParameters(source.getParameters());
        return template;
    }
}
