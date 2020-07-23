package com.sequenceiq.redbeams.controller.v4.database;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
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
@AuthorizationResource
public class DatabaseV4Controller implements DatabaseV4Endpoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Override
    @DisableCheckPermissions
    public DatabaseV4Responses list(String environmentCrn) {
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(databaseConfigService.findAll(environmentCrn),
                DatabaseV4Response.class));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.REGISTER_DATABASE)
    public DatabaseV4Response register(@Valid DatabaseV4Request request) {
        DatabaseConfig databaseConfig = converterUtil.convert(request, DatabaseConfig.class);
        return converterUtil.convert(databaseConfigService.register(databaseConfig, false), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATABASE)
    public DatabaseV4Response getByCrn(@ResourceCrn String crn) {
        DatabaseConfig databaseConfig = databaseConfigService.getByCrn(crn);
        return converterUtil.convert(databaseConfig, DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATABASE)
    public DatabaseV4Response getByName(String environmentCrn, @ResourceName String name) {
        DatabaseConfig databaseConfig = databaseConfigService.getByName(name, environmentCrn);
        return converterUtil.convert(databaseConfig, DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Response deleteByCrn(@ResourceCrn String crn) {
        return converterUtil.convert(databaseConfigService.deleteByCrn(crn), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Response deleteByName(String environmentCrn, @ResourceName String name) {
        return converterUtil.convert(databaseConfigService.deleteByName(name, environmentCrn), DatabaseV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Responses deleteMultiple(@ResourceCrnList Set<String> crns) {
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(databaseConfigService.deleteMultipleByCrn(crns), DatabaseV4Response.class));
    }

    @Override
    @DisableCheckPermissions
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
