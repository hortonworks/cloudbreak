package com.sequenceiq.cloudbreak.converter.v2;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

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
        template.setRootVolumeSize(source.getRootVolumeSize());
        String volumeType = source.getVolumeType();
        template.setVolumeType(volumeType == null ? "HDD" : volumeType);
        template.setCustomInstanceType(source.getCustomInstanceType());
        template.setParameters(source.getParameters());
        if (source.getAwsParameters() != null) {
            template.getParameters().putAll(getConversionService().convert(source.getAwsParameters(), Map.class));
        } else if (source.getAzureParameters() != null) {
            template.getParameters().putAll(getConversionService().convert(source.getAzureParameters(), Map.class));
        } else if (source.getGcpParameters() != null) {
            GcpParameters gcpParameters = Optional.ofNullable(source.getGcpParameters()).orElse(new GcpParameters());
            if (template.getSecretParameters() == null) {
                template.setSecretParameters(getConversionService().convert(gcpParameters, Map.class));
            }
        } else if (source.getOpenStackParameters() != null) {
            template.getParameters().putAll(getConversionService().convert(source.getOpenStackParameters(), Map.class));
        } else if (source.getYarnParameters() != null) {
            template.getParameters().putAll(getConversionService().convert(source.getYarnParameters(), Map.class));
        }
        return template;
    }
}
