package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class JsonToRDSConfigConverter  extends AbstractConversionServiceAwareConverter<RDSConfigJson, RDSConfig> {
    @Override
    public RDSConfig convert(RDSConfigJson source) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(source.getConnectionURL());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setDatabaseType(source.getDatabaseType());
        return rdsConfig;
    }
}