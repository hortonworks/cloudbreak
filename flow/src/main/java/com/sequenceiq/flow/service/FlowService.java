package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toSet;

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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class FlowService {

    private static final Set<String> CANCELLED_TERMINATED_STATES = Sets.newHashSet(FlowConstants.CANCELLED_STATE, FlowConstants.TERMINATED_STATE);

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowService.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Resource
    private Set<String> failHandledEvents;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

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

    public FlowCheckResponse getFlowChainState(String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            List<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            Set<String> relatedFlowIds = flowLogDBService.getFlowIdsByChainIds(relatedChainIds);
            List<FlowLog> relatedFlowLogs = flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(relatedFlowIds);
            flowCheckResponse.setHasActiveFlow(!completed("Flow chain", chainId, relatedChains, relatedFlowLogs));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    public FlowCheckResponse getFlowState(String flowId) {
        List<FlowLog> allByFlowIdOrderByCreatedDesc = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId(flowId);
        flowCheckResponse.setHasActiveFlow(!completed("Flow", flowId, List.of(), allByFlowIdOrderByCreatedDesc));
        return flowCheckResponse;
    }

    private List<FlowChainLog> getRelatedFlowChainLogs(List<FlowChainLog> sourceFlowChains) {
        Optional<FlowChainLog> flowChainWithParent = sourceFlowChains.stream()
                .filter(flowChainLog -> StringUtils.isNotBlank(flowChainLog.getParentFlowChainId())).findFirst();
        FlowChainLog lastFlowChain = sourceFlowChains.stream().sorted(Comparator.comparing(FlowChainLog::getCreated).reversed()).findFirst().get();
        FlowChainLog inputFlowChain = flowChainWithParent.isPresent() ? flowChainWithParent.get() : lastFlowChain;
        return flowChainLogService.collectRelatedFlowChains(inputFlowChain);
    }

    private boolean completed(String marker, String flowChainId, List<FlowChainLog> flowChainLogs, List<FlowLog> flowLogs) {
        if (firstIsPending(flowLogs)) {
            return false;
        }
        boolean hasFinishedFlowLog = false;
        for (FlowLog flowLog : flowLogs) {

            String currentState = flowLog.getCurrentState();
            if (failHandledEvents.contains(flowLog.getNextEvent())
                    || (currentState != null && CANCELLED_TERMINATED_STATES.contains(currentState))) {
                LOGGER.info("{} {} marked as completed on {} flow log", marker, flowChainId, flowLog.minimizedString());
                return true;
            } else if (FlowConstants.INIT_STATE.equals(currentState)) {
                boolean hasEventInQueue = flowChainLogService.hasEventInFlowChainQueue(flowChainLogs);
                LOGGER.info("{} {} state. Finished {}, hasEventInQueue {}", marker, flowChainId, hasFinishedFlowLog, hasEventInQueue);
                return hasFinishedFlowLog && !hasEventInQueue;
            } else if (FlowConstants.FINISHED_STATE.equals(currentState)) {
                hasFinishedFlowLog = true;
            } else if (!hasFinishedFlowLog) {
                return false;
            }
        }
        LOGGER.info("{} {} marked as not completed", marker, flowChainId);
        return false;
    }

    private boolean firstIsPending(List<FlowLog> flowLogs) {
        return !flowLogs.isEmpty() && isPending(flowLogs.get(0));
    }

    private boolean isPending(FlowLog flowLog) {
        return !Boolean.TRUE.equals(flowLog.getFinalized()) || StateStatus.PENDING.equals(flowLog.getStateStatus());
    }
}
