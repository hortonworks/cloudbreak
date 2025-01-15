package com.sequenceiq.redbeams.converter.v4.databaseserver;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.AzureDatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@Component
public class DatabaseServerConfigToDatabasePropertiesV4ResponseConverter {

    public DatabasePropertiesV4Response convert(DatabaseServerConfig source) {
        DatabasePropertiesV4Response response = new DatabasePropertiesV4Response();
        source.getDbStack().ifPresent(dbStack -> {
            if (dbStack.getCloudPlatform().equals(CloudPlatform.AZURE.name())) {
                Json attributes = dbStack.getDatabaseServer().getAttributes();
                Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
                String dbTypeStr = (String) params.get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
                AzureDatabaseType azureDatabaseType =
                        StringUtils.isNotBlank(dbTypeStr) ? AzureDatabaseType.valueOf(dbTypeStr) : AzureDatabaseType.SINGLE_SERVER;
                response.setDatabaseType(azureDatabaseType.name());
                if (azureDatabaseType == AzureDatabaseType.SINGLE_SERVER) {
                    response.setConnectionNameFormat(ConnectionNameFormat.USERNAME_WITH_HOSTNAME);
                }
                AzureDatabasePropertiesV4Response azureProperties = new AzureDatabasePropertiesV4Response();
                dbStack.getDatabaseResources().stream()
                        .filter(dbResource -> dbResource.getResourceType().equals(ResourceType.AZURE_RESOURCE_GROUP))
                        .findFirst()
                        .map(DBResource::getResourceName)
                        .ifPresent(azureProperties::setResourceGroup);
                dbStack.getDatabaseResources().stream()
                        .filter(dbResource -> dbResource.getResourceType().equals(ResourceType.AZURE_DATABASE))
                        .findFirst()
                        .map(DBResource::getResourceReference)
                        .ifPresent(azureProperties::setResourceId);
                response.setAzure(azureProperties);
            }
        });
        return response;
    }
}
