package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;

@Component
public class ConfigQueryEntryToStructuredParameterQueryResponseConverter
        extends AbstractConversionServiceAwareConverter<ConfigQueryEntry, StructuredParameterQueryResponse> {

    @Override
    public StructuredParameterQueryResponse convert(ConfigQueryEntry source) {
        StructuredParameterQueryResponse structuredParameterQueryResponse = new StructuredParameterQueryResponse();
        structuredParameterQueryResponse.setDefaultPath(source.getDefaultPath());
        structuredParameterQueryResponse.setDescription(source.getDescription());
        structuredParameterQueryResponse.setPropertyName(source.getPropertyName());
        structuredParameterQueryResponse.setRelatedService(source.getRelatedService());
        structuredParameterQueryResponse.setPropertyFile(source.getPropertyFile());
        structuredParameterQueryResponse.setProtocol(source.getProtocol());
        structuredParameterQueryResponse.setPropertyDisplayName(source.getPropertyDisplayName());
        return structuredParameterQueryResponse;
    }
}
