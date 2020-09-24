package com.sequenceiq.flow.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.service.FlowService;

@Controller
@AuthorizationResource
public class FlowPublicController implements FlowPublicEndpoint {

    @Inject
    private FlowService flowService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowCheckResponse hasFlowRunningByChainId(String chainId, String resourceCrn) {
        return flowService.getFlowChainStateByResourceCrn(chainId, resourceCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowCheckResponse hasFlowRunningByFlowId(String flowId, String resourceCrn) {
        return flowService.getFlowState(flowId);
    }
}
