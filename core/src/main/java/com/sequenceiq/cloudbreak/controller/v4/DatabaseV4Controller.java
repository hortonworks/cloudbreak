package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.v4.database.DatabaseV4RequestToRDSConfigConverter;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

/**
 * @deprecated obsolete API, should not be used, redbeams is the new service for database operations.
 * Decided not to delete in order to keep API  backward compatibility, added only restriction to call it only with
 * internal actor
 */
@Controller
@InternalOnly
@Deprecated
@Transactional(TxType.NEVER)
@WorkspaceEntityType(RDSConfig.class)
public class DatabaseV4Controller extends NotificationController implements DatabaseV4Endpoint {

    @Inject
    private RdsConfigService databaseService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private RDSConfigToDatabaseV4ResponseConverter rdsConfigToDatabaseV4ResponseConverter;

    @Inject
    private RDSConfigToDatabaseV4RequestConverter rdsConfigToDatabaseV4RequestConverter;

    @Inject
    private DatabaseV4RequestToRDSConfigConverter databaseV4RequestToRDSConfigConverter;

    @Override
    public DatabaseV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<RDSConfig> allInWorkspaceAndEnvironment = databaseService.findAllByWorkspaceId(threadLocalService.getRequestedWorkspaceId());
        return new DatabaseV4Responses(allInWorkspaceAndEnvironment.stream()
                .map(d -> rdsConfigToDatabaseV4ResponseConverter.convert(d))
                .collect(Collectors.toSet())
        );
    }

    @Override
    public DatabaseV4Response get(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, threadLocalService.getRequestedWorkspaceId());
        return rdsConfigToDatabaseV4ResponseConverter.convert(database);
    }

    @Override
    public DatabaseV4Response create(Long workspaceId, DatabaseV4Request request) {
        RDSConfig database = databaseService.createForLoggedInUser(databaseV4RequestToRDSConfigConverter.convert(request),
                threadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RDS_CONFIG_CREATED);
        return rdsConfigToDatabaseV4ResponseConverter.convert(database);
    }

    @Override
    public DatabaseV4Response delete(Long workspaceId, String name) {
        RDSConfig deleted = databaseService.deleteByNameFromWorkspace(name,
                threadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RDS_CONFIG_DELETED);
        return rdsConfigToDatabaseV4ResponseConverter.convert(deleted);
    }

    @Override
    public DatabaseV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<RDSConfig> deleted = databaseService.deleteMultipleByNameFromWorkspace(names, threadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.LDAP_DELETED);
        return new DatabaseV4Responses(deleted.stream()
                .map(d -> rdsConfigToDatabaseV4ResponseConverter.convert(d))
                .collect(Collectors.toSet())
        );
    }

    @Override
    public DatabaseV4Request getRequest(Long workspaceId, String name) {
        RDSConfig database = databaseService.getByNameForWorkspaceId(name, threadLocalService.getRequestedWorkspaceId());
        return rdsConfigToDatabaseV4RequestConverter.convert(database);
    }
}