package com.sequenceiq.distrox.v1.distrox.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Controller
public class DistroXInternalV1Controller implements DistroXInternalV1Endpoint {

    @Inject
    private StackOperations stackOperations;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public StackViewV4Response getByCrn(@TenantAwareParam String crn) {
        return stackOperations.getForInternalCrn(NameOrCrn.ofCrn(crn), StackType.WORKLOAD);
    }
}
