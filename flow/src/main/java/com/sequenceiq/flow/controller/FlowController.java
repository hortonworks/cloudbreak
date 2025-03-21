package com.sequenceiq.flow.controller;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
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
    @AccountIdNotNeeded
    public FlowLogResponse getLastFlowById(String flowId) {
        return flowService.getLastFlowById(flowId);
    }

    @Override
    @AccountIdNotNeeded
    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        return flowService.getFlowLogsByFlowId(flowId);
    }

    @Override
    public FlowLogResponse getLastFlowByResourceName(@AccountId String accountId, String resourceName) {
        return flowService.getLastFlowByResourceName(resourceName);
    }

    @Override
    public FlowLogResponse getLastFlowByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getLastFlowByResourceCrn(resourceCrn);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getFlowLogsByResourceCrn(resourceCrn);
    }

    @Override
    public List<FlowLogResponse> getFlowLogsByResourceName(@AccountId String accountId, String resourceName) {
        return flowService.getFlowLogsByResourceName(resourceName);
    }

    @Override
    @AccountIdNotNeeded
    public FlowCheckResponse hasFlowRunningByChainId(String chainId) {
        return flowService.getFlowChainState(chainId);
    }

    @Override
    @AccountIdNotNeeded
    public FlowCheckResponse hasFlowRunningByFlowId(String flowId) {
        return flowService.getFlowState(flowId);
    }

    @Override
    @AccountIdNotNeeded
    public List<FlowLogResponse> getFlowLogsByFlowIds(List<String> flowIds, int size, int page) {
        Page<FlowLogResponse> response = flowService.getFlowLogsByIds(flowIds, PageRequest.of(page, size));
        return isNull(response) ? newArrayList() : response.getContent();
    }

    @Override
    @AccountIdNotNeeded
    public List<FlowCheckResponse> getFlowChainsStatusesByChainIds(List<String> chainIds, int size, int page) {
        Page<FlowCheckResponse> response = flowService.getFlowChainsByChainIds(chainIds, PageRequest.of(page, size));
        return isNull(response) ? newArrayList() : response.getContent();
    }
}
