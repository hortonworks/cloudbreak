package com.sequenceiq.cloudbreak.controller.v4;


import static com.sequenceiq.authorization.resource.AuthorizationResource.DATAHUB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.workspace.authorization.PermissionCheckingUtils;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
@Transactional(TxType.NEVER)
public class AutoscaleV4Controller implements AutoscaleV4Endpoint {

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Override
    public void putStack(String crn, String userId, @Valid UpdateStackV4Request updateRequest) {
        setupIdentityForAutoscale(crn, userId);
        stackCommonService.putInDefaultWorkspace(crn, updateRequest);
    }

    @Override
    public void putCluster(String crn, String userId, @Valid UpdateClusterV4Request updateRequest) {
        setupIdentityForAutoscale(crn, userId);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        clusterCommonService.put(crn, updateRequest, user, workspace);
    }

    @Override
    public StackV4Response getStackForAmbari(@Valid AmbariAddressV4Request json) {
        return stackCommonService.getStackForAmbari(json);
    }

    @Override
    public AutoscaleStackV4Responses getAllForAutoscale() {
        Set<AutoscaleStackV4Response> allForAutoscale = stackCommonService.getAllForAutoscale();
        return new AutoscaleStackV4Responses(new ArrayList<>(allForAutoscale));
    }

    @Override
    public void failureReport(String crn, FailureReportV4Request failureReport) {
        clusterService.failureReport(crn, failureReport.getFailedNodes());
    }

    @Override
    public StackV4Response get(String crn) {
        return stackCommonService.getByCrn(crn, Collections.emptySet());
    }

    @Override
    public AuthorizeForAutoscaleV4Response authorizeForAutoscale(String crn, String userId, String tenant, String permission) {
        AuthorizeForAutoscaleV4Response response = new AuthorizeForAutoscaleV4Response();
        try {
            restRequestThreadLocalService.setCloudbreakUserByUsernameAndTenant(userId, tenant);
            Stack stack = stackService.getByCrn(crn);
            if (ResourceAction.WRITE.name().equalsIgnoreCase(permission)) {
                User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
                permissionCheckingUtils.checkPermissionForUser(DATAHUB, ResourceAction.WRITE, user.getUserCrn());
            }
            response.setSuccess(true);
        } catch (RuntimeException ignore) {
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    public CertificateV4Response getCertificate(String crn) {
        return stackCommonService.getCertificate(crn);
    }

    private void setupIdentityForAutoscale(String crn, String userId) {
        Tenant tenant = stackService.getTenant(crn);
        restRequestThreadLocalService.setCloudbreakUserByUsernameAndTenant(userId, tenant.getName());
        restRequestThreadLocalService.setRequestedWorkspaceId(stackService.getWorkspaceId(crn));
    }

}
