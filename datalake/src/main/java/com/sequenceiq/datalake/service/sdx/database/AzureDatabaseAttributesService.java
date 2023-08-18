package com.sequenceiq.datalake.service.sdx.database;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;

@Component
public class AzureDatabaseAttributesService {
    @Inject
    private EntitlementService entitlementService;

    public AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        String dbTypeStr = sdxDatabase.getAttributes() != null ?
            (String) sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY) : "";
        if (StringUtils.isNotBlank(dbTypeStr)) {
            return AzureDatabaseType.safeValueOf(dbTypeStr);
        } else {
            return AzureDatabaseType.SINGLE_SERVER;
        }
    }

    public void configureAzureDatabase(SdxDatabase sdxDatabase) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        AzureDatabaseType azureDatabaseType = entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId) ? AzureDatabaseType.FLEXIBLE_SERVER :
                AzureDatabaseType.SINGLE_SERVER;
        Json attributes = sdxDatabase.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
        sdxDatabase.setAttributes(new Json(params));
    }
}
