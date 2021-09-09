package com.sequenceiq.cloudbreak.converter.v4.database;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class DatabaseV4RequestToRDSConfigConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseV4RequestToRDSConfigConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    public RDSConfig convert(DatabaseV4Request source) {
        RDSConfig rdsConfig = new RDSConfig();
        if (Strings.isNullOrEmpty(source.getName())) {
            rdsConfig.setName(missingResourceNameGenerator.generateName(APIResourceType.RDS_CONFIG));
        } else {
            rdsConfig.setName(source.getName());
        }
        rdsConfig.setDescription(source.getDescription());
        rdsConfig.setConnectionURL(source.getConnectionURL());

        DatabaseVendor databaseVendor = DatabaseVendor.getVendorByJdbcUrl(source).get();
        rdsConfig.setDatabaseEngine(databaseVendor);
        rdsConfig.setConnectionDriver(databaseVendor.connectionDriver());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        rdsConfig.setType(source.getType());
        if (rdsConfig.getDatabaseEngine() != DatabaseVendor.POSTGRES && StringUtils.isEmpty(source.getConnectorJarUrl())) {
            String msg = String.format("The 'connectorJarUrl' field needs to be specified for database engine: '%s'.", rdsConfig.getDatabaseEngine());
            LOGGER.info(msg);
        }
        rdsConfig.setConnectorJarUrl(source.getConnectorJarUrl());
        return rdsConfig;
    }

}