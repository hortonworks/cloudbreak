package com.sequenceiq.cloudbreak.controller.v4;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Controller
@Transactional(TxType.NEVER)
@InternalReady
@AuthorizationResource(type = AuthorizationResourceType.DATAHUB)
public class AutoscaleV4Controller implements AutoscaleV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscaleV4Controller.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackOperations stackOperations;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStack(String crn, String userId, @Valid UpdateStackV4Request updateRequest) {
        stackCommonService.putInDefaultWorkspace(crn, updateRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putCluster(String crn, String userId, @Valid UpdateClusterV4Request updateRequest) {
        clusterCommonService.put(crn, updateRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void decommissionInstancesForClusterCrn(String clusterCrn, Long workspaceId,
            List<String> instanceIds, Boolean forced) {
        stackCommonService.deleteMultipleInstancesInWorkspace(NameOrCrn.ofCrn(clusterCrn), workspaceId,
                instanceIds, forced);
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public AutoscaleStackV4Responses getAllForAutoscale() {
        Set<AutoscaleStackV4Response> allForAutoscale = stackCommonService.getAllForAutoscale();
        return new AutoscaleStackV4Responses(new ArrayList<>(allForAutoscale));
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public StackV4Response get(String crn) {
        return stackCommonService.getByCrn(crn, Collections.emptySet());
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public StackStatusV4Response getStatusByCrn(@TenantAwareParam String crn) {
        return stackOperations.getStatus(crn);
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public AuthorizeForAutoscaleV4Response authorizeForAutoscale(String crn, String userId, String tenant, String permission) {
        AuthorizeForAutoscaleV4Response response = new AuthorizeForAutoscaleV4Response();
        try {
            restRequestThreadLocalService.setCloudbreakUserByUsernameAndTenant(userId, tenant);
            // TODO check permission explicitly
            Stack stack = stackService.getByCrn(crn);
            response.setSuccess(true);
        } catch (RuntimeException ignore) {
            response.setSuccess(false);
        }
        return response;
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public CertificateV4Response getCertificate(@TenantAwareParam String crn) {
        return stackCommonService.getCertificate(crn);
    }

    @Override
    @DisableCheckPermissions
    @PreAuthorize("hasRole('AUTOSCALE')")
    public ClusterProxyConfiguration getClusterProxyconfiguration() {
        return clusterProxyService.getClusterProxyConfigurationForAutoscale();
    }
}
