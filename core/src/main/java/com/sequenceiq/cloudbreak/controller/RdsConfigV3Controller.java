package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RdsConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(RDSConfig.class)
public class RdsConfigV3Controller extends AbstractRdsConfigController implements RdsConfigV3Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<RDSConfigResponse> listByWorkspace(Long workspaceId) {
        return getRdsConfigService().findAllByWorkspaceId(workspaceId).stream()
                .map(rdsConfig -> getConversionService().convert(rdsConfig, RDSConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RDSConfigResponse getByNameInWorkspace(Long workspaceId, String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForWorkspaceId(name, workspaceId);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse createInWorkspace(Long workspaceId, RDSConfigRequest request) {
        RDSConfig rdsConfig = getConversionService().convert(request, RDSConfig.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        rdsConfig = getRdsConfigService().create(rdsConfig, workspaceId, user);
        notify(ResourceEvent.RDS_CONFIG_CREATED);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse deleteInWorkspace(Long workspaceId, String name) {
        RDSConfig deleted = getRdsConfigService().deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.RDS_CONFIG_DELETED);
        return getConversionService().convert(deleted, RDSConfigResponse.class);
    }

    @Override
    public RdsTestResult testRdsConnection(Long workspaceId, RDSTestRequest rdsTestRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = getRdsConfigService().getWorkspaceService().get(workspaceId, user);
        return testRdsConnection(rdsTestRequest, workspace);
    }

    @Override
    public RDSConfigRequest getRequestFromName(Long workspaceId, String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForWorkspaceId(name, workspaceId);
        return getConversionService().convert(rdsConfig, RDSConfigRequest.class);
    }
}