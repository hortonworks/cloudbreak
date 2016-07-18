package com.sequenceiq.cloudbreak.converter;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class JsonToRDSConfigConverter extends AbstractConversionServiceAwareConverter<RDSConfigJson, RDSConfig> {
    @Override
    public RDSConfig convert(RDSConfigJson source) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(source.getName());
        rdsConfig.setConnectionURL(source.getConnectionURL());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setDatabaseType(source.getDatabaseType());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        return rdsConfig;
    }
}