package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DbConnectionParamsV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;

@Controller
@InternalOnly
@InternalReady
public class DatabaseConfigV4Controller implements DatabaseConfigV4Endpoint {

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public DbConnectionParamsV4Response getDbConfig(@TenantAwareParam String stackCrn, DatabaseType databaseType) {
        RDSConfig rdsConfig = rdsConfigService.getByStackCrnAndType(stackCrn, databaseType);
        return new DbConnectionParamsV4Response(
                getPath(rdsConfig.getConnectionUserNameSecret()),
                getPath(rdsConfig.getConnectionPasswordSecret())
        );
    }

    private String getPath(String secret) {
        if (secret == null) {
            return null;
        } else {
            try {
                return objectMapper.readValue(secret, VaultSecret.class).getPath();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
