package com.sequenceiq.cloudbreak.converter;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class RDSConfigRequestToRDSConfigConverter extends AbstractConversionServiceAwareConverter<RDSConfigRequest, RDSConfig> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public RDSConfig convert(RDSConfigRequest source) {
        RDSConfig rdsConfig = new RDSConfig();
        if (Strings.isNullOrEmpty(source.getName())) {
            rdsConfig.setName(missingResourceNameGenerator.generateName(APIResourceType.RDS_CONFIG));
        } else {
            rdsConfig.setName(source.getName());
        }
        rdsConfig.setConnectionURL(source.getConnectionURL());
        rdsConfig.setDatabaseEngine(source.getDatabaseEngine());
        rdsConfig.setConnectionDriver(source.getConnectionDriver());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        rdsConfig.setType(source.getType());
        return rdsConfig;
    }
}