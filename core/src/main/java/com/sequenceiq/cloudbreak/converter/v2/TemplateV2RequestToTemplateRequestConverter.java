package com.sequenceiq.cloudbreak.converter.v2;

import static java.util.Collections.emptyMap;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
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
        template.getParameters().putAll(getTemplateParameters(source).asMap());
        return template;
    }

    private BaseTemplateParameter getTemplateParameters(TemplateV2Request source) {
        if (source.getAwsTemplateParameters() != null) {
            return source.getAwsTemplateParameters();
        } else if (source.getAzureTemplateParameters() != null) {
            return source.getAzureTemplateParameters();
        } else if (source.getGcpTemlateParameters() != null) {
            return source.getGcpTemlateParameters();
        } else if (source.getOpenStackTemplateParameters() != null) {
            return source.getOpenStackTemplateParameters();
        } else {
            return new BaseTemplateParameter() {
                @Override
                public Map<String, Object> asMap() {
                    return emptyMap();
                }
            };
        }
    }
}
