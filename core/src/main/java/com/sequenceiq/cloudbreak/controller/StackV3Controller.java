package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.GeneratedBlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ReinstallRequestV2;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
@Transactional(TxType.NEVER)
public class StackV3Controller extends NotificationController implements StackV3Endpoint {

    @Inject
    private StackCommonService stackCommonService;

    @Inject
    private ClusterCommonService clusterCommonService;

    @Inject
    private StackService stackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<StackResponse> listByOrganization(Long organizationId) {
        return stackCommonService.retrieveStacksByOrganizationId(organizationId);
    }

    @Override
    public StackResponse getByNameInOrganization(Long organizationId, String name) {
        return stackCommonService.findStackByNameAndOrganizationId(name, organizationId);
    }

    @Override
    public StackResponse createInOrganization(Long organizationId, StackV2Request request) {
        return stackCommonService.createInOrganization(conversionService.convert(request, StackRequest.class), organizationId);
    }

    @Override
    public void deleteInOrganization(Long organizationId, String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteInOrganization(name, organizationId, forced, deleteDependencies);
    }

    @Override
    public Response putSyncInOrganization(Long organizationId, String name) {
        return stackCommonService.putSyncInOrganization(name, organizationId);
    }

    @Override
    public void retryInOrganization(Long organizationId, String name) {
        stackCommonService.retryInOrganization(name, organizationId);
    }

    @Override
    public Response putStopInOrganization(Long organizationId, String name) {
        return stackCommonService.putStopInOrganization(name, organizationId);
    }

    @Override
    public Response putStartInOrganization(Long organizationId, String name) {
        return stackCommonService.putStartInOrganization(name, organizationId);
    }

    @Override
    public Response putScalingInOrganization(Long organizationId, String name, @Valid StackScaleRequestV2 updateRequest) {
        return stackCommonService.putScalingInOrganization(name, organizationId, updateRequest);
    }

    @Override
    public Response repairClusterInOrganization(Long organizationId, String name, ClusterRepairRequest clusterRepairRequest) {
        stackCommonService.repairCluster(organizationId, name, clusterRepairRequest);
        return Response.accepted().build();
    }

    @Override
    public void deleteWithKerberosInOrg(Long organizationId, String name, Boolean withStackDelete, Boolean deleteDependencies) {
        stackCommonService.deleteWithKerbereosInOrg(name, organizationId, withStackDelete, deleteDependencies);
    }

    @Override
    public StackV2Request getRequestfromName(Long organizationId, String name) {
        return stackService.getStackRequestByNameInOrg(name, organizationId);
    }

    @Override
    public GeneratedBlueprintResponse postStackForBlueprint(Long organizationId, String name, @Valid StackV2Request stackRequest) {
        return stackCommonService.postStackForBlueprint(stackRequest);
    }

    @Override
    public Response deleteInstance(Long organizationId, String name, String instanceId, boolean forced) {
        return stackCommonService.deleteInstanceByNameInOrg(name, organizationId, instanceId, forced);
    }

    @Override
    public Response changeImage(Long organizationId, String name, @Valid StackImageChangeRequest stackImageChangeRequest) {
        return stackCommonService.changeImageByNameInOrg(name, organizationId, stackImageChangeRequest);
    }

    @Override
    public Response putReinstall(Long organizationId, String name, @Valid ReinstallRequestV2 reinstallRequestV2) {
        Stack stack = stackService.getByNameInOrg(name, organizationId);
        UpdateClusterJson updateClusterJson = conversionService.convert(reinstallRequestV2, UpdateClusterJson.class);
        return clusterCommonService.put(stack.getId(), updateClusterJson);
    }
}
