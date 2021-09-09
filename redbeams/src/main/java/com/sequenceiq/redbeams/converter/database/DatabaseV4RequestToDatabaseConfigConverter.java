package com.sequenceiq.redbeams.converter.database;


import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.util.DatabaseVendorUtil;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@Component
public class DatabaseV4RequestToDatabaseConfigConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseV4RequestToDatabaseConfigConverter.class);

    @Inject
    private Clock clock;

    @Inject
    private DatabaseVendorUtil databaseVendorUtil;

    public DatabaseConfig convert(DatabaseV4Request source) {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName(source.getName());
        databaseConfig.setDescription(source.getDescription());
        databaseConfig.setConnectionURL(source.getConnectionURL());

        DatabaseVendor databaseVendor = databaseVendorUtil.getVendorByJdbcUrl(source.getConnectionURL()).get();
        databaseConfig.setDatabaseVendor(databaseVendor);
        databaseConfig.setConnectionDriver(source.getConnectionDriver());
        databaseConfig.setConnectionUserName(source.getConnectionUserName());
        databaseConfig.setConnectionPassword(source.getConnectionPassword());
        databaseConfig.setCreationDate(clock.getCurrentTimeMillis());
        databaseConfig.setStatus(ResourceStatus.USER_MANAGED);
        databaseConfig.setType(source.getType());
        databaseConfig.setEnvironmentId(source.getEnvironmentCrn());

        return databaseConfig;
    }
}
