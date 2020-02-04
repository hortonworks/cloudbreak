package com.sequenceiq.flow.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@Controller
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
    public List<FlowLogResponse> getFlowLogsByResourceNameAndChainId(String resourceName, String chainId) {
        return flowService.getFlowLogsByResourceNameAndChainId(resourceName, chainId);
    }

    @Override
    public FlowCheckResponse hasFlowRunning(String resourceName, String chainId) {
        return flowService.hasFlowRunning(resourceName, chainId);
    }
}
