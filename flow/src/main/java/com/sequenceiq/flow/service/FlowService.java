package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;
import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.isFlowInFailedState;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.converter.FlowLogConverter;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.domain.ClassValue;
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
    private FlowProgressResponseConverter flowProgressResponseConverter;

    @Inject
    private FlowLogConverter flowLogConverter;

    @Inject
    private FlowOperationStatisticsService flowOperationStatisticsService;

    public FlowLogResponse getLastFlowById(String flowId) {
        LOGGER.info("Getting last flow log by flow id {}", flowId);
        Optional<FlowLog> lastFlowLog = flowLogDBService.getLastFlowLog(flowId);
        if (lastFlowLog.isPresent()) {
            return flowLogConverter.convert(lastFlowLog.get());
        }
        throw new BadRequestException("Not found flow for this flow id!");
    }

    public List<FlowLogResponse> getFlowLogsByFlowId(String flowId) {
        LOGGER.info("Getting flow logs by flow id {}", flowId);
        List<FlowLog> flowLogs = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        return flowLogs.stream().map(flowLog -> flowLogConverter.convert(flowLog)).collect(Collectors.toList());
    }

    public FlowLogResponse getLastFlowByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting last flow log by resource name {}", resourceName);
        return flowLogConverter.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceName));
    }

    public FlowLogResponse getLastFlowByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting last flow log by resource crn {}", resourceCrn);
        return flowLogConverter.convert(flowLogDBService.getLastFlowLogByResourceCrnOrName(resourceCrn));
    }

    public List<FlowLogResponse> getFlowLogsByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        return flowLogs.stream().map(flowLog -> flowLogConverter.convert(flowLog)).collect(Collectors.toList());
    }

    public <T extends AbstractFlowConfiguration> List<FlowLogResponse> getFlowLogsByCrnAndType(String resourceCrn, ClassValue classValue) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs by resource crn {} and type {}", resourceCrn, classValue.getClassValue().getCanonicalName());
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByCrnAndType(resourceCrn, classValue);
        return flowLogs.stream().map(flowLog -> flowLogConverter.convert(flowLog)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting flow logs by resource name {}", resourceName);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> flowLogConverter.convert(flowLog)).collect(Collectors.toList());
    }

    public FlowCheckResponse getFlowChainStateByResourceCrn(String chainId, String resourceCrn) {
        Crn crn;
        List<Long> resourceIds;
        try {
            crn = Crn.safeFromString(resourceCrn);
            if (crn.getService().equals(Crn.Service.DATALAKE)) {
                resourceIds = flowLogDBService.getResourceIdsByCrn(resourceCrn);
            } else {
                Long resourceId = flowLogDBService.getResourceIdByCrnOrName(resourceCrn);
                resourceIds = Collections.singletonList(resourceId);
            }
        } catch (CrnParseException crnParseException) {
            resourceIds = Collections.EMPTY_LIST;
        }

        return getFlowChainStateSafe(resourceIds, chainId);
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
            flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(relatedFlowLogs, failHandledEvents));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    public FlowCheckResponse getFlowChainStateSafe(List<Long> resourceIdList, String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            List<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            Set<String> relatedFlowIds = flowLogDBService.getFlowIdsByChainIds(relatedChainIds);
            List<FlowLog> relatedFlowLogs = flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(relatedFlowIds);
            validateResourceId(relatedFlowLogs, resourceIdList);
            flowCheckResponse.setHasActiveFlow(!completed("Flow chain", chainId, relatedChains, relatedFlowLogs));
            flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(relatedFlowLogs, failHandledEvents));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    private void validateResourceId(List<FlowLog> flowLogs, List<Long> resourceIdList) {
        if (flowLogs.stream().anyMatch(flowLog -> !resourceIdList.contains(flowLog.getResourceId()))) {
            throw new BadRequestException(String.format("The requested chain id %s does not belong to that " +
                    "resources %s", flowLogs.get(0).getFlowChainId(), resourceIdList));
        }
    }

    public FlowCheckResponse getFlowState(String flowId) {
        List<FlowLog> allByFlowIdOrderByCreatedDesc = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId(flowId);
        flowCheckResponse.setHasActiveFlow(!completed("Flow", flowId, List.of(), allByFlowIdOrderByCreatedDesc));
        flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(allByFlowIdOrderByCreatedDesc, failHandledEvents));
        return flowCheckResponse;
    }

    private List<FlowChainLog> getRelatedFlowChainLogs(List<FlowChainLog> sourceFlowChains) {
        Optional<FlowChainLog> flowChainWithParent = sourceFlowChains.stream()
                .filter(flowChainLog -> StringUtils.isNotBlank(flowChainLog.getParentFlowChainId())).findFirst();
        FlowChainLog lastFlowChain = sourceFlowChains.stream().max(Comparator.comparing(FlowChainLog::getCreated)).get();
        FlowChainLog inputFlowChain = flowChainWithParent.orElse(lastFlowChain);
        return flowChainLogService.collectRelatedFlowChains(inputFlowChain);
    }

    private boolean completed(String marker, String flowChainId, List<FlowChainLog> flowChainLogs, List<FlowLog> flowLogs) {
        if (firstIsPending(flowLogs)) {
            return false;
        }
        if (isFlowInFailedState(flowLogs, failHandledEvents)) {
            return true;
        }
        return hasFinishedFlow(marker, flowChainId, flowChainLogs, flowLogs);
    }

    private boolean hasFinishedFlow(String marker, String flowChainId, List<FlowChainLog> flowChainLogs, List<FlowLog> flowLogs) {
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

    public FlowProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceCrn);
        FlowProgressResponse response = flowProgressResponseConverter.convert(flowLogs, resourceCrn);
        if (StringUtils.isBlank(response.getFlowId())) {
            throw new NotFoundException(String.format("Not found any historical flow data for requested resource (crn: %s)", resourceCrn));
        }
        return flowProgressResponseConverter.convert(flowLogs, resourceCrn);
    }

    public List<FlowProgressResponse> getFlowProgressListByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) for all recent flows by resource crn {}", resourceCrn);
        List<FlowLog> flowLogs = flowLogDBService.getAllFlowLogsByResourceCrnOrName(resourceCrn);
        return flowProgressResponseConverter.convertList(flowLogs, resourceCrn);
    }

    public <T extends AbstractFlowConfiguration> Optional<FlowProgressResponse> getLastFlowProgressByResourceCrnAndType(
            String resourceCrn, ClassValue classValue) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) by resource crn {} and type {}", resourceCrn, classValue.getClassValue().getCanonicalName());
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByCrnAndType(resourceCrn, classValue);
        FlowProgressResponse response = flowProgressResponseConverter.convert(flowLogs, resourceCrn);
        if (StringUtils.isBlank(response.getFlowId())) {
            LOGGER.debug("Not found any historical flow data for requested resource (crn: {})", resourceCrn);
            return Optional.empty();
        }
        return Optional.ofNullable(flowProgressResponseConverter.convert(flowLogs, resourceCrn));
    }

    public Optional<OperationFlowsView> getLastFlowOperationByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnInFlowChain(resourceCrn);
        return flowOperationStatisticsService.createOperationResponse(resourceCrn, flowLogs);
    }
}
