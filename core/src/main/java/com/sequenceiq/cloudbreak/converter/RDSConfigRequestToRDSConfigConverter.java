package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class RDSConfigRequestToRDSConfigConverter extends AbstractConversionServiceAwareConverter<RDSConfigRequest, RDSConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigRequestToRDSConfigConverter.class);

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

        Optional<DatabaseVendor> databaseVendor = DatabaseVendor.getVendorByJdbcUrl(source);
        if (databaseVendor.isPresent()) {
            rdsConfig.setDatabaseEngine(databaseVendor.get().name());
            rdsConfig.setConnectionDriver(databaseVendor.get().connectionDriver());
        } else {
            throw new BadRequestException("Not a valid DatabaseVendor which was provided in the jdbc url.");
        }
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        rdsConfig.setType(source.getType());

        if (!rdsConfig.getDatabaseEngine().equalsIgnoreCase(DatabaseVendor.POSTGRES.name()) && StringUtils.isEmpty(source.getConnectorJarUrl())) {
            String msg = String.format("The 'connectorJarUrl' field needs to be specified for database engine: '%s'.", rdsConfig.getDatabaseEngine());
            LOGGER.warn(msg);
        }
        rdsConfig.setConnectorJarUrl(source.getConnectorJarUrl());
        return rdsConfig;
    }
}