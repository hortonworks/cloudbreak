package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigToJsonConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigJson> {
    @Override
    public RDSConfigJson convert(RDSConfig source) {
        RDSConfigJson json = new RDSConfigJson();
        json.setConnectionURL(source.getConnectionURL());
        json.setConnectionUserName(source.getConnectionUserName());
        json.setConnectionPassword(source.getConnectionPassword());
        json.setDatabaseType(source.getDatabaseType());
        return json;
    }
}
