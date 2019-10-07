package com.sequenceiq.cloudbreak.controller;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.ChangedNodesReport;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
@Transactional(TxType.NEVER)
public class AutoscaleController implements AutoscaleEndpoint {

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Override
    public Response putStack(Long id, String owner, @Valid UpdateStackJson updateRequest) {
        setupIdentityForAutoscale(id, owner);
        return stackCommonService.putInDefaultWorkspace(id, updateRequest);
    }

    private void setupIdentityForAutoscale(Long id, String owner) {
        restRequestThreadLocalService.setCloudbreakUserByOwner(owner);
        restRequestThreadLocalService.setRequestedWorkspaceId(stackService.getWorkspaceId(id));
    }

    @Override
    public Response putCluster(Long stackId, String owner, @Valid UpdateClusterJson updateRequest) {
        setupIdentityForAutoscale(stackId, owner);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return clusterCommonService.put(stackId, updateRequest, user, workspace);
    }

    @Override
    public StackResponse getStackForAmbari(@Valid AmbariAddressJson json) {
        return stackCommonService.getStackForAmbari(json);
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        return stackCommonService.getAllForAutoscale();
    }

    @Override
    public Response failureReport(Long stackId, FailureReport failureReport) {
        return clusterService.reportHealthChange(stackId, Set.copyOf(failureReport.getFailedNodes()), Set.of());
    }

    @Override
    public Response changedNodesReport(Long stackId, ChangedNodesReport changedNodesReport) {
        return clusterService.reportHealthChange(stackId,
                Set.copyOf(changedNodesReport.getNewFailedNodes()),
                Set.copyOf(changedNodesReport.getNewHealthyNodes()));
    }

    @Override
    public StackResponse get(Long id) {
        return stackCommonService.get(id, Collections.emptySet());
    }

    @Override
    public AutoscaleClusterResponse getForAutoscale(Long stackId) {
        Stack stack = stackService.getForAutoscale(stackId);
        AutoscaleClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stackId, AutoscaleClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return stackCommonService.getCertificate(stackId);
    }
}
