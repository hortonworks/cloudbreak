package com.sequenceiq.datalake.service.sdx.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class AzureDatabaseAttributesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseAttributesService.class);

    @Inject
    private EntitlementService entitlementService;

    public AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        String dbTypeStr = "";
        AzureDatabaseType azureDatabaseType = AzureDatabaseType.SINGLE_SERVER;
        if (sdxDatabase.getAttributes() != null) {
            dbTypeStr = (String) sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
        }
        if (StringUtils.isNotBlank(dbTypeStr)) {
            azureDatabaseType = AzureDatabaseType.safeValueOf(dbTypeStr);
        }
        return azureDatabaseType;
    }

    public void configureAzureDatabase(SdxDatabaseRequest databaseRequest, SdxDatabase sdxDatabase) {
        AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getAzureDatabaseType)
                .orElse(AzureDatabaseType.SINGLE_SERVER);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER && !entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId)) {
            LOGGER.info("Azure Flexible Database Server creation is not entitled for {} account.", accountId);
            throw new BadRequestException("You are not entitled to use Flexible Database Server on Azure for your cluster." +
                    " Please contact Cloudera to enable " + Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER + " for your account");
        }
        Json attributes = sdxDatabase.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
        sdxDatabase.setAttributes(new Json(params));
    }
}
