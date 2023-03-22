package com.sequenceiq.datalake.service.sdx.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class AzureDatabaseAttributesService {
    public AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        String dbTypeStr = "";
        AzureDatabaseType azureDatabaseType = AzureDatabaseType.SINGLE_SERVER;
        if (sdxDatabase.getAttributes() != null) {
            dbTypeStr = (String) sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
        }
        if (StringUtils.isNotBlank(dbTypeStr)) {
            azureDatabaseType = AzureDatabaseType.valueOf(dbTypeStr);
        }
        return azureDatabaseType;
    }

    public void configureAzureDatabase(SdxDatabaseRequest databaseRequest, SdxDatabase sdxDatabase) {
        AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getAzureDatabaseType)
                .orElse(AzureDatabaseType.SINGLE_SERVER);
        Json attributes = sdxDatabase.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
        sdxDatabase.setAttributes(new Json(params));
    }
}
