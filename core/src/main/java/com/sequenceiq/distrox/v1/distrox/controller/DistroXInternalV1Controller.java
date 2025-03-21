package com.sequenceiq.distrox.v1.distrox.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StatusCrnsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackInstancesV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DistroXInternalV1Controller implements DistroXInternalV1Endpoint {

    @Inject
    private StackOperations stackOperations;

    @Inject
    private StackOperationService stackOperationService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public StackViewV4Response getByCrn(@ResourceCrn String crn) {
        return stackOperations.getForInternalCrn(NameOrCrn.ofCrn(crn), StackType.WORKLOAD);
    }

    @Override
    @InternalOnly
    public StackInstancesV4Responses getInstancesByCrn(@ResourceCrn String crn) {
        return stackOperations.getInstancesForInternalCrn(crn, StackType.WORKLOAD);
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public StackStatusV4Responses getStatusByCrns(@RequestObject StatusCrnsV4Request request) {
        return stackOperations.getStatusForInternalCrns(request.getCrns(), StackType.WORKLOAD);
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewCertificate(@ResourceCrn String crn) {
        return stackOperationService.renewInternalCertificate(NameOrCrn.ofCrn(crn), StackType.WORKLOAD);
    }
}
