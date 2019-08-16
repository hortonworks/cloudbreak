package com.sequenceiq.redbeams.converter.v4.databaseserver;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Component
public class DatabaseServerConfigToDatabaseServerV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<DatabaseServerConfig, DatabaseServerV4Response> {

    @Override
    public DatabaseServerV4Response convert(DatabaseServerConfig source) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(source.getId());
        response.setCrn(source.getResourceCrn().toString());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setHost(source.getHost());
        response.setPort(source.getPort());

        response.setDatabaseVendor(source.getDatabaseVendor().databaseType());
        response.setDatabaseVendorDisplayName(source.getDatabaseVendor().displayName());
        response.setConnectionDriver(source.getConnectionDriver());
        response.setConnectionUserName(getConversionService().convert(source.getConnectionUserNameSecret(), SecretResponse.class));
        response.setConnectionPassword(getConversionService().convert(source.getConnectionPasswordSecret(), SecretResponse.class));

        response.setCreationDate(source.getCreationDate());

        response.setEnvironmentCrn(source.getEnvironmentId());

        response.setResourceStatus(source.getResourceStatus());
        if (source.getDbStack().isPresent()) {
            DBStack dbStack = source.getDbStack().get();
            response.setStatus(dbStack.getStatus());
            response.setStatusReason(dbStack.getStatusReason());
        } else if (source.getHost() != null && source.getPort() != null) {
            response.setStatus(Status.AVAILABLE);
        } else {
            response.setStatus(Status.UNKNOWN);
        }

        return response;
    }
}
