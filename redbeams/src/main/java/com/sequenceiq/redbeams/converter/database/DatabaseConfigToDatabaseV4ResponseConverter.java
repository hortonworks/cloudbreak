package com.sequenceiq.redbeams.converter.database;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@Component
public class DatabaseConfigToDatabaseV4ResponseConverter {

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    public DatabaseV4Response convert(DatabaseConfig source) {
        DatabaseV4Response json = new DatabaseV4Response();
        json.setName(source.getName());
        json.setCrn(source.getResourceCrn().toString());
        json.setDescription(source.getDescription());
        json.setConnectionURL(source.getConnectionURL());
        json.setDatabaseEngine(source.getDatabaseVendor().name());
        json.setConnectionDriver(source.getConnectionDriver());
        json.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserName().getSecret()));
        json.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPassword().getSecret()));
        json.setDatabaseEngineDisplayName(source.getDatabaseVendor().displayName());
        json.setCreationDate(source.getCreationDate());
        json.setEnvironmentCrn(source.getEnvironmentId());
        json.setType(source.getType());
        json.setResourceStatus(source.getStatus());
        return json;
    }
}
