package com.sequenceiq.flow.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@Controller
@InternalOnly
public class FlowController implements FlowEndpoint {

    @Inject
    private FlowService flowService;

    @Override
    public FlowLogResponse getLastFlowById(String flowId) {
        return flowService.getLastFlowById(flowId);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        return flowService.getFlowLogsByFlowId(flowId);
    }

    @Override
    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        return flowService.getLastFlowByResourceName(resourceName);
    }

    @Override
    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        return flowService.getLastFlowByResourceCrn(resourceCrn);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        return flowService.getFlowLogsByResourceCrn(resourceCrn);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        return flowService.getFlowLogsByResourceName(resourceName);
    }

    @Override
    public FlowCheckResponse hasFlowRunningByChainId(String chainId) {
        return flowService.getFlowChainState(chainId);
    }

    @Override
    public FlowCheckResponse hasFlowRunningByFlowId(String flowId) {
        return flowService.getFlowState(flowId);
    }
}
