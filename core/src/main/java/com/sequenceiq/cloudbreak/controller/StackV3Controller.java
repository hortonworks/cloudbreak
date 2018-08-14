package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.GeneratedBlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;

@Component
@Transactional(TxType.NEVER)
public class StackV3Controller extends NotificationController implements StackV3Endpoint {

    @Override
    public Set<StackResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public StackResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public StackResponse createInOrganization(Long organizationId, StackV2Request request) {
        return null;
    }

    @Override
    public StackResponse deleteInOrganization(Long organizationId, String name, Boolean forced, Boolean deleteDependencies) {
        return null;
    }

    @Override
    public StackResponse putSyncInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public void retryInOrganization(Long organizationId, String name) {

    }

    @Override
    public StackResponse putStopInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public StackResponse putStartInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public StackResponse putScalingInOrganization(Long organizationId, String name, @Valid StackScaleRequestV2 updateRequest) {
        return null;
    }

    @Override
    public StackResponse repairClusterInOrganization(Long organizationId, String name, ClusterRepairRequest clusterRepairRequest) {
        return null;
    }

    @Override
    public void deleteWithKerberosInOrg(Long organizationId, String name, Boolean withStackDelete, Boolean deleteDependencies) {

    }

    @Override
    public StackV2Request getRequestfromName(Long organizationId, String name) {
        return null;
    }

    @Override
    public GeneratedBlueprintResponse postStackForBlueprint(Long organizationId, String name, @Valid StackV2Request stackRequest) {
        return null;
    }

    @Override
    public StackResponse deleteInstance(Long organizationId, String name, Long stackId, String instanceId, boolean forced) {
        return null;
    }

    @Override
    public Response changeImage(Long organizationId, String name, @Valid StackImageChangeRequest stackImageChangeRequest) {
        return null;
    }
}
