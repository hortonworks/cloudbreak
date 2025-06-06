package com.sequenceiq.redbeams.controller.v4.database;

import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.converter.database.DatabaseConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.redbeams.converter.database.DatabaseV4RequestToDatabaseConfigConverter;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class DatabaseV4Controller implements DatabaseV4Endpoint {

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Inject
    private DatabaseConfigToDatabaseV4ResponseConverter databaseConfigToDatabaseV4ResponseConverter;

    @Inject
    private DatabaseV4RequestToDatabaseConfigConverter databaseV4RequestToDatabaseConfigConverter;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DatabaseV4Responses list(@ResourceCrn String environmentCrn) {
        return new DatabaseV4Responses(databaseConfigService.findAll(environmentCrn).stream()
                .map(d -> databaseConfigToDatabaseV4ResponseConverter.convert(d))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.REGISTER_DATABASE)
    public DatabaseV4Response register(DatabaseV4Request request) {
        DatabaseConfig databaseConfig = databaseV4RequestToDatabaseConfigConverter.convert(request);
        return databaseConfigToDatabaseV4ResponseConverter.convert(databaseConfigService.register(databaseConfig, false));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATABASE)
    public DatabaseV4Response getByCrn(@ResourceCrn String crn) {
        DatabaseConfig databaseConfig = databaseConfigService.getByCrn(crn);
        return databaseConfigToDatabaseV4ResponseConverter.convert(databaseConfig);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATABASE)
    public DatabaseV4Response getByName(@ResourceCrn String environmentCrn, @ResourceName String name) {
        DatabaseConfig databaseConfig = databaseConfigService.getByName(name, environmentCrn);
        return databaseConfigToDatabaseV4ResponseConverter.convert(databaseConfig);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Response deleteByCrn(@ResourceCrn String crn) {
        return databaseConfigToDatabaseV4ResponseConverter.convert(databaseConfigService.deleteByCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Response deleteByName(@ResourceCrn String environmentCrn, @ResourceName String name) {
        return databaseConfigToDatabaseV4ResponseConverter.convert(databaseConfigService.deleteByName(name, environmentCrn));
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_DATABASE)
    public DatabaseV4Responses deleteMultiple(@ResourceCrnList Set<String> crns) {
        return new DatabaseV4Responses(databaseConfigService.deleteMultipleByCrn(crns).stream()
                .map(d -> databaseConfigToDatabaseV4ResponseConverter.convert(d))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByRequestProperty(path = "existingDatabase.name", type = NAME, action = AuthorizationResourceAction.DESCRIBE_DATABASE)
    public DatabaseTestV4Response test(@RequestObject DatabaseTestV4Request databaseTestV4Request) {
        throw new UnsupportedOperationException("Connection testing is disabled for security reasons until further notice");
    }
}
