package com.sequenceiq.redbeams.converter.v4.databaseserver;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Component
public class DatabaseServerV4RequestToDatabaseServerConfigConverter {

    public DatabaseServerConfig convert(DatabaseServerV4Request source) {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setName(source.getName());
        server.setDescription(source.getDescription());
        server.setHost(source.getHost());
        server.setPort(source.getPort());

        DatabaseVendor databaseVendor = DatabaseVendor.fromValue(source.getDatabaseVendor());
        server.setDatabaseVendor(databaseVendor);
        server.setConnectionDriver(source.getConnectionDriver());
        server.setConnectionUserName(source.getConnectionUserName());
        server.setConnectionPassword(source.getConnectionPassword());

        server.setResourceStatus(ResourceStatus.USER_MANAGED);

        server.setArchived(false);
        server.setDeletionTimestamp(null);

        server.setEnvironmentId(source.getEnvironmentCrn());

        return server;
    }

}
