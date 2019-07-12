package com.sequenceiq.redbeams.converter.v4.databaseserver;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Component
public class DatabaseServerConfigToDatabaseServerV4RequestConverter
    extends AbstractConversionServiceAwareConverter<DatabaseServerConfig, DatabaseServerV4Request> {

    @Override
    public DatabaseServerV4Request convert(DatabaseServerConfig source) {
        DatabaseServerV4Request request = new DatabaseServerV4Request();
        request.setName(source.getName());
        request.setDescription(source.getDescription());
        request.setHost(source.getHost());
        request.setPort(source.getPort());

        request.setDatabaseVendor(source.getDatabaseVendor().databaseType());
        request.setConnectionUserName("REDACTED");
        request.setConnectionPassword("REDACTED");
        request.setConnectorJarUrl(source.getConnectorJarUrl());

        request.setEnvironmentCrn(source.getEnvironmentId());

        return request;
    }
}
