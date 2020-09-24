package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DbConnectionParamsV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.DatabaseConfigEndpoint;

@Controller
@InternalOnly
public class DatabaseConfigController implements DatabaseConfigEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private DatabaseConfigV4Endpoint databaseConfigV4Endpoint;

    @Override
    public DbConnectionParamsV4Response getDbConfig(@TenantAwareParam String datalakeCrn, DatabaseType databaseType) {
        String stackCrn = sdxService.getStackCrnByClusterCrn(datalakeCrn);
        return databaseConfigV4Endpoint.getDbConfig(stackCrn, databaseType);
    }
}
