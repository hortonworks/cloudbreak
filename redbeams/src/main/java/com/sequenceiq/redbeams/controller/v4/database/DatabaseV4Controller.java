package com.sequenceiq.redbeams.controller.v4.database;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AuthorizationResource(type = AuthorizationResourceType.DATALAKE)
public class DatabaseV4Controller implements DatabaseV4Endpoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DatabaseV4Responses list(String environmentCrn) {
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(databaseConfigService.findAll(environmentCrn),
                DatabaseV4Response.class));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DatabaseV4Response register(@Valid DatabaseV4Request request) {
        DatabaseConfig databaseConfig = converterUtil.convert(request, DatabaseConfig.class);
        return converterUtil.convert(databaseConfigService.register(databaseConfig, false), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DatabaseV4Response getByCrn(String crn) {
        DatabaseConfig databaseConfig = databaseConfigService.getByCrn(crn);
        return converterUtil.convert(databaseConfig, DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DatabaseV4Response getByName(String environmentCrn, String name) {
        DatabaseConfig databaseConfig = databaseConfigService.getByName(name, environmentCrn);
        return converterUtil.convert(databaseConfig, DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DatabaseV4Response deleteByCrn(String crn) {
        return converterUtil.convert(databaseConfigService.deleteByCrn(crn), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DatabaseV4Response deleteByName(String environmentCrn, String name) {
        return converterUtil.convert(databaseConfigService.deleteByName(name, environmentCrn), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public DatabaseV4Responses deleteMultiple(Set<String> crns) {
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(databaseConfigService.deleteMultipleByCrn(crns), DatabaseV4Response.class));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DatabaseTestV4Response test(@Valid DatabaseTestV4Request databaseTestV4Request) {
        throw new UnsupportedOperationException("Connection testing is disabled for security reasons until further notice");
        // String result = "";
        // if (databaseTestV4Request.getExistingDatabase() != null) {
        //     result = databaseConfigService.testConnection(
        //             databaseTestV4Request.getExistingDatabase().getName(),
        //             databaseTestV4Request.getExistingDatabase().getEnvironmentCrn()
        //     );
        // } else {
        //     DatabaseConfig databaseConfig = converterUtil.convert(databaseTestV4Request.getDatabase(), DatabaseConfig.class);
        //     result = databaseConfigService.testConnection(databaseConfig);
        // }
        // return new DatabaseTestV4Response(result);
    }
}
