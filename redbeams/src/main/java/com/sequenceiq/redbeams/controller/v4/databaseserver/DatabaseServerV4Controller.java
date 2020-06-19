package com.sequenceiq.redbeams.controller.v4.databaseserver;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.converter.stack.AllocateDatabaseServerV4RequestToDBStackConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.RedbeamsCreationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStartService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStopService;
import com.sequenceiq.redbeams.service.stack.RedbeamsTerminationService;

@Controller
@AuthorizationResource
@Transactional(TxType.NEVER)
public class DatabaseServerV4Controller implements DatabaseServerV4Endpoint {

    static final Long DEFAULT_WORKSPACE = 0L;

    @Inject
    private RedbeamsCreationService redbeamsCreationService;

    @Inject
    private RedbeamsTerminationService redbeamsTerminationService;

    @Inject
    private RedbeamsStartService redbeamsStartService;

    @Inject
    private RedbeamsStopService redbeamsStopService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private AllocateDatabaseServerV4RequestToDBStackConverter dbStackConverter;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerV4Responses list(String environmentCrn) {
        Set<DatabaseServerConfig> all = databaseServerConfigService.findAll(DEFAULT_WORKSPACE, environmentCrn);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(all, DatabaseServerV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerV4Response getByName(String environmentCrn, @ResourceName String name) {
        DatabaseServerConfig server = databaseServerConfigService.getByName(DEFAULT_WORKSPACE, environmentCrn, name);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER)
    public DatabaseServerV4Response getByCrn(@ResourceCrn String crn) {
        DatabaseServerConfig server = databaseServerConfigService.getByCrn(crn);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATABASE_SERVER)
    public DatabaseServerStatusV4Response create(AllocateDatabaseServerV4Request request) {
        DBStack dbStack = dbStackConverter.convert(request, ThreadBasedUserCrnProvider.getUserCrn());
        DBStack savedDBStack = redbeamsCreationService.launchDatabaseServer(dbStack, request.getClusterCrn());
        return converterUtil.convert(savedDBStack, DatabaseServerStatusV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response release(@ResourceCrn String crn) {
        DatabaseServerConfig server = databaseServerConfigService.release(crn);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @CheckPermissionByAccount(action = AuthorizationResourceAction.REGISTER_DATABASE_SERVER)
    public DatabaseServerV4Response register(DatabaseServerV4Request request) {
        DatabaseServerConfig server = databaseServerConfigService.create(converterUtil.convert(request, DatabaseServerConfig.class), DEFAULT_WORKSPACE, false);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_CREATED);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response deleteByCrn(@ResourceCrn String crn, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        DatabaseServerConfig deleted = redbeamsTerminationService.terminateByCrn(crn, force);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return converterUtil.convert(deleted, DatabaseServerV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Response deleteByName(String environmentCrn, @ResourceName String name, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        DatabaseServerConfig deleted = redbeamsTerminationService.terminateByName(environmentCrn, name, force);
        return converterUtil.convert(deleted, DatabaseServerV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_DATABASE_SERVER)
    public DatabaseServerV4Responses deleteMultiple(@ResourceCrnList Set<String> crns, boolean force) {
        // RedbeamsTerminationService handles both service-managed and user-managed database servers
        Set<DatabaseServerConfig> deleted = redbeamsTerminationService.terminateMultipleByCrn(crns, force);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(deleted, DatabaseServerV4Response.class));
    }

    @Override
    @DisableCheckPermissions
    public DatabaseServerTestV4Response test(DatabaseServerTestV4Request request) {
        throw new UnsupportedOperationException("Connection testing is disabled for security reasons until further notice");
        // String connectionResult;
        // if (request.getExistingDatabaseServerCrn() != null) {
        //     connectionResult = databaseServerConfigService.testConnection(request.getExistingDatabaseServerCrn());
        // } else {
        //     DatabaseServerConfig server = converterUtil.convert(request.getDatabaseServer(), DatabaseServerConfig.class);
        //     connectionResult = databaseServerConfigService.testConnection(server);
        // }
        // return new DatabaseServerTestV4Response(connectionResult);
    }

    @Override
    @CheckPermissionByResourceObject
    public CreateDatabaseV4Response createDatabase(@ResourceObject CreateDatabaseV4Request request) {
        String result = databaseServerConfigService.createDatabaseOnServer(
                request.getExistingDatabaseServerCrn(),
                request.getDatabaseName(),
                request.getType(),
                Optional.ofNullable(request.getDatabaseDescription()));
        return new CreateDatabaseV4Response(result);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATABASE_SERVER)
    public void start(@ResourceCrn @ValidCrn @NotNull String crn) {
        redbeamsStartService.startDatabaseServer(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATABASE_SERVER)
    public void stop(@ResourceCrn @ValidCrn @NotNull String crn) {
        redbeamsStopService.stopDatabaseServer(crn);
    }
}
