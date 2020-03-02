package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@DisableCheckPermissions
@Transactional(TxType.NEVER)
@WorkspaceEntityType(RDSConfig.class)
public class DatabaseV4Controller extends NotificationController implements DatabaseV4Endpoint {

    @Inject
    private RdsConfigService databaseService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public DatabaseV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<RDSConfig> allInWorkspaceAndEnvironment = databaseService.findAllByWorkspaceId(workspaceId);
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, DatabaseV4Response.class));
    }

    @Override
    public DatabaseV4Response get(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(database, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response create(Long workspaceId, DatabaseV4Request request) {
        RDSConfig database = databaseService.createForLoggedInUser(converterUtil.convert(request, RDSConfig.class), workspaceId);
        notify(ResourceEvent.RDS_CONFIG_CREATED);
        return converterUtil.convert(database, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response delete(Long workspaceId, String name) {
        RDSConfig deleted = databaseService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.RDS_CONFIG_DELETED);
        return converterUtil.convert(deleted, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<RDSConfig> deleted = databaseService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.LDAP_DELETED);
        return new DatabaseV4Responses(converterUtil.convertAllAsSet(deleted, DatabaseV4Response.class));
    }

    @Override
    public DatabaseV4Request getRequest(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(database, DatabaseV4Request.class);
    }
}