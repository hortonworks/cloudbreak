package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

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

    @Resource
    private List<String> failHandledEvents;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    private Set<FlowChainLog> getRelatedFlowChainLogs(List<FlowChainLog> sourceFlowChains) {
        Optional<FlowChainLog> flowChainWithParent = sourceFlowChains.stream()
                .filter(flowChainLog -> StringUtils.isNotBlank(flowChainLog.getParentFlowChainId())).findFirst();
        FlowChainLog lastFlowChain = sourceFlowChains.stream().sorted(Comparator.comparing(FlowChainLog::getCreated).reversed()).findFirst().get();
        FlowChainLog inputFlowChain = flowChainWithParent.isPresent() ? flowChainWithParent.get() : lastFlowChain;
        return flowChainLogService.collectRelatedFlowChains(inputFlowChain);
    }

    public FlowLogResponse getLastFlowById(String flowId) {
        LOGGER.info("Getting last flow log by flow id {}", flowId);
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(flowId);
        if (lastFlowLog.isPresent()) {
            return conversionService.convert(lastFlowLog.get(), FlowLogResponse.class);
        }
        throw new BadRequestException("Not found flow for this flow id!");
    }

    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        LOGGER.info("Getting flow logs by flow id {}", flowId);
        List<FlowLog> flowLogs = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting last flow log by resource name {}", resourceName);
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceName), FlowLogResponse.class);
    }

    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting last flow log by resource crn {}", resourceCrn);
        return conversionService.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceCrn), FlowLogResponse.class);
    }

    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting flow logs by resource name {}", resourceName);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowCheckResponse hasFlowRunningByChainId(String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            Set<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            List<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(Collectors.toList());
            List<FlowLog> relatedFlowLogs = flowLogDBService.getFlowLogsByChainIds(relatedChainIds);
            Optional<FlowLog> lastFlowLog = relatedFlowLogs.stream().max(Comparator.comparing(FlowLog::getCreated));
            if (lastFlowLog.isPresent()) {
                if (failHandledEvents.contains(lastFlowLog.get().getNextEvent())) {
                    flowCheckResponse.setHasActiveFlow(false);
                    return flowCheckResponse;
                }
            }
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
