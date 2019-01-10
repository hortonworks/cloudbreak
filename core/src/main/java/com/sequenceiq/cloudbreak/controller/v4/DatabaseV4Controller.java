package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.filter.DatabaseV4ListFilter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(RDSConfig.class)
public class DatabaseV4Controller extends NotificationController implements DatabaseV4Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private RdsConfigService databaseService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public GeneralSetV4Response<DatabaseV4Response> list(Long workspaceId, DatabaseV4ListFilter databaseV4ListFilter) {
        Set<DatabaseV4Response> databaseV4Respons = databaseService.findAllInWorkspaceAndEnvironment(workspaceId,
                databaseV4ListFilter.getEnvironment(), databaseV4ListFilter.getAttachGlobal()).stream()
                .map(database -> conversionService.convert(database, DatabaseV4Response.class))
                .collect(Collectors.toSet());
        return GeneralSetV4Response.propagateResponses(databaseV4Respons);
    }

    @Override
    public DatabaseV4Response get(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(database, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response create(Long workspaceId, DatabaseV4Request request) {
        RDSConfig database = conversionService.convert(request, RDSConfig.class);
        database = databaseService.createInEnvironment(database, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.RDS_CONFIG_CREATED);
        return conversionService.convert(database, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response delete(Long workspaceId, String name) {
        RDSConfig deleted = databaseService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.RDS_CONFIG_DELETED);
        return conversionService.convert(deleted, DatabaseV4Response.class);
    }

    @Override
    public DatabaseTestV4Response test(Long workspaceId, DatabaseTestV4Request databaseTestV4Request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = databaseService.getWorkspaceService().get(workspaceId, user);
        String existingRDSConfigName = databaseTestV4Request.getExistingDatabaseName();
        DatabaseV4Request configRequest = databaseTestV4Request.getDatabase();
        if (existingRDSConfigName != null) {
            return new DatabaseTestV4Response(databaseService.testRdsConnection(existingRDSConfigName, workspace));
        } else if (configRequest != null) {
            RDSConfig rdsConfig = conversionService.convert(configRequest, RDSConfig.class);
            return new DatabaseTestV4Response(databaseService.testRdsConnection(rdsConfig));
        }
        throw new BadRequestException("Either an Database id, name or an Database request needs to be specified in the request. ");
    }

    @Override
    public DatabaseV4Request getRequest(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(database, DatabaseV4Request.class);
    }

    @Override
    public DatabaseV4Response attach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return databaseService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response detach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return databaseService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, DatabaseV4Response.class);
    }
}