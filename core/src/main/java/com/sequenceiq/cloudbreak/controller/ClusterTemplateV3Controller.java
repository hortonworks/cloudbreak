package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ClusterTemplateV3EndPoint;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackFromTemplateRequest;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.converter.mapper.ClusterTemplateMapper;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV3Controller extends NotificationController implements ClusterTemplateV3EndPoint {

    @Inject
    private ClusterTemplateMapper clusterTemplateMapper;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public ClusterTemplateResponse createInWorkspace(Long workspaceId, @Valid ClusterTemplateRequest request) {
        credentialValidator.validateCredentialCloudPlatform(request.getCloudPlatform());
        ClusterTemplate clusterTemplate = clusterTemplateMapper.mapRequestToEntity(request);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        clusterTemplate = clusterTemplateService.create(clusterTemplate, workspaceId, user);
        return clusterTemplateMapper.mapEntityToResponse(clusterTemplate);
    }

    @Override
    public StackResponse createStackInWorkspaceFromTemplate(Long workspaceId, String name, @Valid StackFromTemplateRequest request) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        return stackCommonService.createInWorkspaceFromTemplate(name, request, cloudbreakUser, user, workspace);
    }

    @Override
    public Set<ClusterTemplateResponse> listByWorkspace(Long workspaceId) {
        return clusterTemplateMapper.mapEntityToResponse(clusterTemplateService.findAllByWorkspaceId(workspaceId));
    }

    @Override
    public ClusterTemplateResponse getByNameInWorkspace(Long workspaceId, String name) {
        return clusterTemplateMapper.mapEntityToResponse(clusterTemplateService.getByNameForWorkspaceId(name, workspaceId));
    }

    @Override
    public void deleteInWorkspace(Long workspaceId, String name) {
        clusterTemplateService.deleteByNameFromWorkspace(name, workspaceId);
    }
}
