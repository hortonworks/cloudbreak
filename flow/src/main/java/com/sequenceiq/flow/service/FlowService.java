package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class FlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowService.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public FlowLogResponse getLastFlowById(String flowId) {
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(flowId);
        if (lastFlowLog.isPresent()) {
            return conversionService.convert(lastFlowLog.get(), FlowLogResponse.class);
        }
        throw new BadRequestException("Not found flow for this flow id!");
    }

    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        List<FlowLog> flowLogs = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceName), FlowLogResponse.class);
    }

    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceCrn), FlowLogResponse.class);
    }

    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowCheckResponse hasFlowRunningByChainId(String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        Optional<FlowChainLog> firstByFlowChainIdOrderByCreatedDesc = flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(chainId);
        if (firstByFlowChainIdOrderByCreatedDesc.isPresent()) {
            List<FlowChainLog> relatedChains = flowChainLogService.collectRelatedFlowChains(Lists.newArrayList(), firstByFlowChainIdOrderByCreatedDesc.get());
            List<FlowLog> relatedFlowLogs = flowLogDBService.
                    getFlowLogsByChainIds(relatedChains.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList()));
            flowCheckResponse.setHasActiveFlow(flowChainLogService.checkIfAnyFlowChainHasEventInQueue(relatedChains) ||
                    flowLogDBService.hasPendingFlowEvent(relatedFlowLogs));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    public FlowCheckResponse hasFlowRunningByFlowId(String flowId) {
        List<FlowLog> allByFlowIdOrderByCreatedDesc = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId(flowId);
        flowCheckResponse.setHasActiveFlow(flowLogDBService.hasPendingFlowEvent(allByFlowIdOrderByCreatedDesc));
        return flowCheckResponse;
    }
}
