package com.sequenceiq.redbeams.converter.v4.databaseserver;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Component
public class DatabaseServerV4RequestToDatabaseServerConfigConverter
    extends AbstractConversionServiceAwareConverter<DatabaseServerV4Request, DatabaseServerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerV4RequestToDatabaseServerConfigConverter.class);

    @Override
    public DatabaseServerConfig convert(DatabaseServerV4Request source) {
        DatabaseServerConfig server = new DatabaseServerConfig();
        if (Strings.isNullOrEmpty(source.getName())) {
            server.setName(generateName());
        } else {
            server.setName(source.getName());
        }
        server.setDescription(source.getDescription());
        server.setHost(source.getHost());
        server.setPort(source.getPort());

        DatabaseVendor databaseVendor = DatabaseVendor.fromValue(source.getDatabaseVendor());
        server.setDatabaseVendor(databaseVendor);
        server.setConnectionDriver(databaseVendor.connectionDriver());
        server.setConnectionUserName(source.getConnectionUserName());
        server.setConnectionPassword(source.getConnectionPassword());

        server.setResourceStatus(ResourceStatus.USER_MANAGED);

        if (server.getDatabaseVendor() != DatabaseVendor.POSTGRES && Strings.isNullOrEmpty(source.getConnectorJarUrl())) {
            String msg = String.format("The 'connectorJarUrl' field needs to be specified for database vendor '%s'.", server.getDatabaseVendor());
            LOGGER.info(msg);
            // FIXME: should this fail?
        }
        server.setConnectorJarUrl(source.getConnectorJarUrl());

        server.setArchived(false);
        server.setDeletionTimestamp(null);

        server.setEnvironmentId(source.getEnvironmentId());

        return server;
    }

    // Sorry, MissingResourceNameGenerator seems like overkill
    private static String generateName() {
        return String.format("dbsvr-%s", UUID.randomUUID().toString());
    }
}
