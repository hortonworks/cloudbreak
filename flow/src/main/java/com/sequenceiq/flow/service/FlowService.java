package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;
import static com.sequenceiq.flow.domain.StateStatus.FAILED;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
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

    @Inject
    private FlowProgressResponseConverter flowProgressResponseConverter;

    @Inject
    private FlowStatCache flowStatCache;

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

    public <T extends AbstractFlowConfiguration> List<FlowLogResponse> getFlowLogsByCrnAndType(String resourceCrn, Class<T> clazz) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs by resource crn {} and type {}", resourceCrn, clazz.getCanonicalName());
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByCrnAndType(resourceCrn, clazz);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public List<FlowLogResponse> getFlowLogsByResourceName(String resourceName) {
        checkState(!Crn.isCrn(resourceName));
        LOGGER.info("Getting flow logs by resource name {}", resourceName);
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByResourceCrnOrName(resourceName);
        return flowLogs.stream().map(flowLog -> conversionService.convert(flowLog, FlowLogResponse.class)).collect(Collectors.toList());
    }

    public FlowCheckResponse getFlowChainStateByResourceCrn(String chainId, String resourceCrn) {
        Long resourceId = flowLogDBService.getResourceIdByCrnOrName(resourceCrn);
        return getFlowChainStateSafe(resourceId, chainId);
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
            flowCheckResponse.setLatestFlowFinalizedAndFailed(firstIsFinalizedAndFailed(relatedFlowLogs));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    public FlowCheckResponse getFlowChainStateSafe(Long resourceId, String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            List<FlowChainLog> relatedChains = getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            Set<String> relatedFlowIds = flowLogDBService.getFlowIdsByChainIds(relatedChainIds);
            List<FlowLog> relatedFlowLogs = flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(relatedFlowIds);
            validateResourceId(relatedFlowLogs, resourceId);
            flowCheckResponse.setHasActiveFlow(!completed("Flow chain", chainId, relatedChains, relatedFlowLogs));
            flowCheckResponse.setLatestFlowFinalizedAndFailed(firstIsFinalizedAndFailed(relatedFlowLogs));
            return flowCheckResponse;
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
            return flowCheckResponse;
        }
    }

    private void validateResourceId(List<FlowLog> flowlogs, Long resourceId) {
        if (flowlogs.stream().anyMatch(l -> !l.getResourceId().equals(resourceId))) {
            throw new BadRequestException(String.format("The requested chain id %s does not belong to that resource", resourceId));
        }
    }

    public FlowCheckResponse getFlowState(String flowId) {
        List<FlowLog> allByFlowIdOrderByCreatedDesc = flowLogDBService.findAllByFlowIdOrderByCreatedDesc(flowId);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId(flowId);
        flowCheckResponse.setHasActiveFlow(!completed("Flow", flowId, List.of(), allByFlowIdOrderByCreatedDesc));
        flowCheckResponse.setLatestFlowFinalizedAndFailed(firstIsFinalizedAndFailed(allByFlowIdOrderByCreatedDesc));
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
        if (firstIsFinalizedAndFailed(flowLogs)) {
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

    private boolean firstIsFinalizedAndFailed(List<FlowLog> flowLogs) {
        if (!flowLogs.isEmpty()) {
            FlowLog recentFlowLog = flowLogs.get(0);
            return recentFlowLog.getFinalized() && FAILED.equals(recentFlowLog.getStateStatus());
        }
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

    public <T extends AbstractFlowConfiguration> Optional<FlowProgressResponse> getLastFlowProgressByResourceCrnAndType(String resourceCrn, Class<T> clazz) {
        checkState(Crn.isCrn(resourceCrn));
        LOGGER.info("Getting flow logs (progress) by resource crn {} and type {}", resourceCrn, clazz.getCanonicalName());
        List<FlowLog> flowLogs = flowLogDBService.getFlowLogsByCrnAndType(resourceCrn, clazz);
        FlowProgressResponse response = flowProgressResponseConverter.convert(flowLogs, resourceCrn);
        if (StringUtils.isBlank(response.getFlowId())) {
            LOGGER.debug("Not found any historical flow data for requested resource (crn: {})", resourceCrn);
            return Optional.empty();
        }
        return Optional.ofNullable(flowProgressResponseConverter.convert(flowLogs, resourceCrn));
    }

    public Optional<OperationFlowsView> getLastFlowOperationByResourceCrn(String resourceCrn) {
        checkState(Crn.isCrn(resourceCrn));
        Optional<OperationFlowsView> operationFlowsView = flowStatCache.getOperationFlowByResourceCrn(resourceCrn);
        if (operationFlowsView.isPresent()) {
            return operationFlowsView;
        }
        List<FlowLog> flowLogs = flowLogDBService.getLatestFlowLogsByCrnInFlowChain(resourceCrn);
        OperationType operationType = flowLogs.isEmpty() ? OperationType.UNKNOWN : flowLogs.get(0).getOperationType();
        Map<String, List<FlowLog>> flowLogsPerType = flowLogs.stream()
                .filter(fl -> fl.getFlowType() != null)
                .collect(groupingBy(fl -> fl.getFlowType().getCanonicalName()));
        Map<String, FlowProgressResponse> response = new HashMap<>();
        Map<Long, String> createdMap = new TreeMap<>();
        List<String> typeOrderList = new ArrayList<>();
        for (Map.Entry<String, List<FlowLog>> entry : flowLogsPerType.entrySet()) {
            FlowProgressResponse progressResponse = flowProgressResponseConverter.convert(entry.getValue(), resourceCrn);
            response.put(entry.getKey(), progressResponse);
            createdMap.put(progressResponse.getCreated(), entry.getKey());
        }
        for (Map.Entry<Long, String> entry : createdMap.entrySet()) {
            typeOrderList.add(entry.getValue());
        }
        if (CollectionUtils.isEmpty(response.entrySet())) {
            LOGGER.debug("Not found any historical flow data for requested resource (crn: {})", resourceCrn);
            return Optional.empty();
        }
        return Optional.of(OperationFlowsView.Builder.newBuilder()
                .withOperationType(operationType)
                .withFlowTypeProgressMap(response)
                .withInMemory(false)
                .withTypeOrderList(typeOrderList)
                .build());
    }
}
