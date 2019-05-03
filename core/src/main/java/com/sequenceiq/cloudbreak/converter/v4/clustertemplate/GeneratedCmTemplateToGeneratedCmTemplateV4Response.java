package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class GeneratedCmTemplateToGeneratedCmTemplateV4Response
        extends AbstractConversionServiceAwareConverter<GeneratedCmTemplate, GeneratedCmTemplateV4Response> {

    @Override
    public GeneratedCmTemplateV4Response convert(GeneratedCmTemplate source) {
        GeneratedCmTemplateV4Response generatedCmTemplateV4Response = new GeneratedCmTemplateV4Response();
        generatedCmTemplateV4Response.setTemplate(source.getTemplate());
        return generatedCmTemplateV4Response;
    }
}
