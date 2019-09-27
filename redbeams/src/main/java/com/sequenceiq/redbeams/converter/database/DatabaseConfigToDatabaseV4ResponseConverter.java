package com.sequenceiq.redbeams.converter.database;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@Component
public class DatabaseConfigToDatabaseV4ResponseConverter extends AbstractConversionServiceAwareConverter<DatabaseConfig, DatabaseV4Response> {

    @Override
    public DatabaseV4Response convert(DatabaseConfig source) {
        DatabaseV4Response json = new DatabaseV4Response();
        json.setName(source.getName());
        json.setCrn(source.getResourceCrn().toString());
        json.setDescription(source.getDescription());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseVendor().name());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setConnectionUserName(getConversionService().convert(source.getConnectionUserName().getSecret(), SecretResponse.class));
        json.setConnectionPassword(getConversionService().convert(source.getConnectionPassword().getSecret(), SecretResponse.class));
        json.setDatabaseEngineDisplayName(source.getDatabaseVendor().displayName());
        json.setCreationDate(source.getCreationDate());
        json.setEnvironmentCrn(source.getEnvironmentId());
        json.setType(source.getType());
        json.setResourceStatus(source.getStatus());
        return json;
    }
}
