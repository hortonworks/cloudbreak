package com.sequenceiq.cloudbreak.converter.v4.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StructuredParameterQueryV4Response;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;

@Component
public class ConfigQueryEntryToStructuredParameterQueryV4ResponseConverter {

    public StructuredParameterQueryV4Response convert(ConfigQueryEntry source) {
        StructuredParameterQueryV4Response structuredParameterQueryV4Response = new StructuredParameterQueryV4Response();
        structuredParameterQueryV4Response.setType(source.getType().name());
        structuredParameterQueryV4Response.setDefaultPath(source.getDefaultPath());
        structuredParameterQueryV4Response.setDescription(source.getDescription());
        structuredParameterQueryV4Response.setPropertyName(source.getPropertyName());
        structuredParameterQueryV4Response.setRelatedServices(source.getRelatedServices());
        structuredParameterQueryV4Response.setRelatedMissingServices(source.getRelatedMissingServices());
        structuredParameterQueryV4Response.setPropertyFile(source.getPropertyFile());
        structuredParameterQueryV4Response.setProtocol(source.getProtocol());
        structuredParameterQueryV4Response.setPropertyDisplayName(source.getPropertyDisplayName());
        return structuredParameterQueryV4Response;
    }
}
