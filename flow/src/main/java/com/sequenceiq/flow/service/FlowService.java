package com.sequenceiq.flow.service;

import static com.google.common.base.Preconditions.checkState;
import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.isFlowInFailedState;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.converter.FlowLogConverter;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
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
    private FlowLogConverter flowLogConverter;

    @Inject
    private ResourceIdProvider resourceIdProvider;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    public boolean isPreviousFlowFailed(Long stackId, String flowChainId) {
        if (flowChainId != null) {
            FlowCheckResponse flowCheckResponse = getFlowChainStateSafe(List.of(stackId), flowChainId);
            return !flowCheckResponse.getHasActiveFlow() && flowCheckResponse.getLatestFlowFinalizedAndFailed();
        }
        return false;
    }

    public FlowLogResponse getLastFlowById(String flowId) {
        LOGGER.info("Getting last flow log by flow id {}", flowId);
        Optional<FlowLog> lastFlowLog = flowLogDBService.findFirstByFlowIdOrderByCreatedDesc(flowId);
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

    public List<FlowLogResponse> getAllFlowLogsByResourceCrnAndFlowTypes(String resourceCrn, List<ClassValue> flowTypes) {
        checkState(Crn.isCrn(resourceCrn));
        List<String> flowTypeNames = flowTypes.stream().map(ClassValue::getSimpleName).toList();
        LOGGER.info("Getting all flow logs by resource crn {} and flow types: {}", resourceCrn, String.join(", ", flowTypeNames));
        List<FlowLog> flowLogs = flowLogDBService.getAllFlowLogsByResourceCrnOrNameAndFlowTypes(resourceCrn, flowTypes);
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
            resourceIds = Collections.emptyList();
        }

        return getFlowChainStateSafe(resourceIds, chainId);
    }

    public FlowCheckResponse getFlowChainState(String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            List<FlowChainLog> relatedChains = flowChainLogService.getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            List<FlowLogWithoutPayload> relatedFlowLogs = flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(relatedChainIds);
            flowCheckResponse.setHasActiveFlow(!completed(FlowConstants.FLOW_CHAIN, chainId, relatedChains, relatedFlowLogs));
            flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(relatedFlowLogs, failHandledEvents));
            setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);
            setStateEventFlowTypeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs, FlowConstants.FLOW_CHAIN, chainId);
            flowCheckResponse.setReason(getLatestFlowReason(relatedFlowLogs));
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
        }
        return flowCheckResponse;
    }

    public void setEndTimeOnFlowCheckResponse(FlowCheckResponse flowCheckResponse, List<FlowLogWithoutPayload> relatedFlowLogs) {
        if (!flowCheckResponse.getHasActiveFlow() && !relatedFlowLogs.isEmpty()) {
            Map<String, Optional<FlowLogWithoutPayload>> latestFlowsByCreatedMap = relatedFlowLogs.stream()
                    .filter(s -> !Objects.equals(s.getCurrentState(), "FINISHED"))
                    .collect(groupingBy(FlowLogWithoutPayload::getFlowId,
                            Collectors.reducing(BinaryOperator.maxBy(Comparator.comparing(FlowLogWithoutPayload::getCreated)))));
            List<FlowLogWithoutPayload> flowLogForEndTime = latestFlowsByCreatedMap.values().stream()
                    .filter(Optional::isPresent).map(Optional::get).collect(toList());
            Optional<Long> flowEndTime = flowLogForEndTime.stream().map(FlowLogWithoutPayload::getEndTime)
                    .filter(Objects::nonNull).max(Long::compareTo);
            if (flowEndTime.isPresent()) {
                flowCheckResponse.setEndTime(flowEndTime.get());
            }
        }
    }

    public FlowCheckResponse getFlowChainStateSafe(List<Long> resourceIdList, String chainId) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainId);
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(chainId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("Checking if there is an active flow based on flow chain id {}", chainId);
            List<FlowChainLog> relatedChains = flowChainLogService.getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            List<FlowLogWithoutPayload> relatedFlowLogs = flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(relatedChainIds);
            validateResourceId(relatedFlowLogs, resourceIdList);
            flowCheckResponse.setHasActiveFlow(!completed(FlowConstants.FLOW_CHAIN, chainId, relatedChains, relatedFlowLogs));
            flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(relatedFlowLogs, failHandledEvents));
            flowCheckResponse.setReason(getLatestFlowReason(relatedFlowLogs));
        } else {
            flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
        }
        return flowCheckResponse;
    }

    private void validateResourceId(List<FlowLogWithoutPayload> flowLogs, List<Long> resourceIdList) {
        if (flowLogs.stream().anyMatch(flowLog -> !resourceIdList.contains(flowLog.getResourceId()))) {
            throw new BadRequestException(String.format("The requested chain id %s does not belong to that " +
                    "resources %s", flowLogs.getFirst().getFlowChainId(), resourceIdList));
        }
    }

    public FlowCheckResponse getFlowState(String flowId) {
        List<FlowLogWithoutPayload> allByFlowIdOrderByCreatedDesc = flowLogDBService.findAllWithoutPayloadByFlowIdOrderByCreatedDesc(flowId);
        if (allByFlowIdOrderByCreatedDesc.isEmpty()) {
            throw new NotFoundException(String.format("Flow '%s' not found.", flowId));
        }
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowId(flowId);
        flowCheckResponse.setHasActiveFlow(!completed(FlowConstants.FLOW, flowId, List.of(), allByFlowIdOrderByCreatedDesc));
        flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(allByFlowIdOrderByCreatedDesc, failHandledEvents));
        flowCheckResponse.setReason(getLatestFlowReason(allByFlowIdOrderByCreatedDesc));
        setStateEventFlowTypeOnFlowCheckResponse(flowCheckResponse, allByFlowIdOrderByCreatedDesc, FlowConstants.FLOW, flowId);
        return flowCheckResponse;
    }

    public OperationStatusResponse getOperationStatus(String resourceCrn, String operationId) {
        Objects.requireNonNull(resourceCrn, "resourceCrn must not be null");
        Preconditions.checkState(Crn.isCrn(resourceCrn), "resourceCrn is not a valid crn");
        Long resourceId = resourceIdProvider.getResourceIdByResourceCrn(resourceCrn);
        if (operationId != null) {
            LOGGER.info("Get operation status. Resource crn: {}, operation id: {}", resourceCrn, operationId);
            OperationStatusResponse operationByFlowId = getOperationByFlowId(operationId, resourceId);
            if (operationByFlowId != null) {
                return operationByFlowId;
            }
            OperationStatusResponse operationByFlowChainId = getOperationByFlowChainId(operationId, resourceId, false);
            if (operationByFlowChainId != null) {
                return operationByFlowChainId;
            } else {
                throw new NotFoundException("Not found operation.");
            }
        } else {
            LOGGER.info("Get operation status. Resource crn: {}", resourceCrn);
            return flowLogDBService.getLastFlowLog(resourceId)
                    .map(lastFlowLog -> {
                        if (lastFlowLog.getFlowChainId() != null) {
                            return getOperationByFlowChainId(lastFlowLog.getFlowChainId(), resourceId, true);
                        } else {
                            return getOperationByFlowId(lastFlowLog.getFlowId(), resourceId);
                        }
                    }).orElseThrow(() -> new NotFoundException("Not found operation."));
        }
    }

    private OperationStatusResponse getOperationByFlowId(String operationId, Long resourceId) {
        List<FlowLogWithoutPayload> flowLogs = flowLogDBService.findAllWithoutPayloadByFlowIdOrderByCreatedDesc(operationId);
        if (!flowLogs.isEmpty()) {
            LOGGER.info("{} operation is running in a flow.", operationId);
            validateResourceId(flowLogs, List.of(resourceId));
            boolean completed = completed(FlowConstants.FLOW, operationId, List.of(), flowLogs);
            boolean flowIsInFailedState = isFlowInFailedState(flowLogs, failHandledEvents);
            Long created = flowLogs.getLast().getCreated();
            Long endTime = flowLogs.getFirst().getEndTime();
            String name = getNameFromFlowType(flowLogs.getFirst().getFlowType());
            return convertToOperationResponse(operationId, name, completed, flowIsInFailedState, created, endTime);
        } else {
            return null;
        }
    }

    private String getNameFromFlowType(ClassValue flowType) {
        return flowConfigs.stream()
                .filter(flowConfiguration -> flowType.getName().equals(flowConfiguration.getClass().getName()))
                .findFirst()
                .map(FlowConfiguration::getOperationName)
                .orElse(flowType.getSimpleName().replace("FlowConfig", ""));
    }

    private OperationStatusResponse getOperationByFlowChainId(String operationId, Long resourceId, boolean returnRootFlowChain) {
        List<FlowChainLog> flowChains = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(operationId);
        if (!flowChains.isEmpty()) {
            LOGGER.info("{} operation is running in a flow chain.", operationId);
            List<FlowChainLog> relatedChains = flowChainLogService.getRelatedFlowChainLogs(flowChains);
            Set<String> relatedChainIds = relatedChains.stream().map(FlowChainLog::getFlowChainId).collect(toSet());
            List<FlowLogWithoutPayload> relatedFlowLogs = flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(relatedChainIds);
            validateResourceId(relatedFlowLogs, List.of(resourceId));
            boolean completed = completed(FlowConstants.FLOW_CHAIN, operationId, relatedChains, relatedFlowLogs);
            boolean flowIsInFailedState = isFlowInFailedState(relatedFlowLogs, failHandledEvents);
            Long created = relatedFlowLogs.getLast().getCreated();
            Long endTime = relatedFlowLogs.getFirst().getEndTime();
            String flowChainId = returnRootFlowChain ? relatedChains.stream()
                    .filter(chain -> StringUtils.isEmpty(chain.getParentFlowChainId()))
                    .map(FlowChainLog::getFlowChainId)
                    .findFirst()
                    .orElse(operationId) : operationId;
            String name = returnRootFlowChain ? relatedChains.stream()
                    .filter(chain -> StringUtils.isEmpty(chain.getParentFlowChainId()))
                    .map(FlowChainLog::getFlowChainType)
                    .findFirst()
                    .orElse(flowChains.getFirst().getFlowChainType()) : flowChains.getFirst().getFlowChainType();
            return convertToOperationResponse(flowChainId, getNameFromFlowChainType(name), completed, flowIsInFailedState, created, endTime);
        } else {
            return null;
        }
    }

    private String getNameFromFlowChainType(String name) {
        return flowChainFactories
                .stream()
                .filter(flowEventChainFactory -> flowEventChainFactory.getClass().getSimpleName().equals(name))
                .findFirst()
                .map(FlowEventChainFactory::getOperationName)
                .orElse(name.replace("FlowEventChainFactory", ""));
    }

    private OperationStatusResponse convertToOperationResponse(String operationId, String name, boolean completed, boolean failed, Long startTime,
            Long endTime) {
        OperationStatusResponse operation = new OperationStatusResponse();
        operation.setOperationId(operationId);
        operation.setOperationName(name);
        if (startTime != null) {
            operation.setStarted(convertToUtcDate(startTime));
        }
        if (completed && endTime != null) {
            operation.setEnded(convertToUtcDate(endTime));
        }
        operation.setOperationStatus(!completed ? OperationProgressStatus.RUNNING : failed ? OperationProgressStatus.FAILED : OperationProgressStatus.FINISHED);
        return operation;
    }

    private static OffsetDateTime convertToUtcDate(Long created) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(created), ZoneOffset.UTC);
    }

    private String getLatestFlowReason(List<FlowLogWithoutPayload> flowLogs) {
        String latestFlowLogId = flowLogs.stream().map(FlowLogWithoutPayload::getFlowId).findFirst().orElse(null);
        return flowLogs.stream()
                .filter(flogLog -> flogLog.getFlowId().equals(latestFlowLogId))
                .filter(flowLog -> StringUtils.isNotBlank(flowLog.getReason()))
                .findFirst()
                .map(FlowLogWithoutPayload::getReason)
                .orElse(null);
    }

    private void setStateEventFlowTypeOnFlowCheckResponse(FlowCheckResponse flowCheckResponse, List<FlowLogWithoutPayload> relatedFlowLogs,
            String marker, String id) {
        if ((flowCheckResponse.getHasActiveFlow() || flowCheckResponse.getLatestFlowFinalizedAndFailed()) && !relatedFlowLogs.isEmpty()) {
            LOGGER.info("Setting current state, next event, and flow type into flow check response using {} {}", marker, id);
            FlowLogWithoutPayload latestFlowLog = relatedFlowLogs.get(0);
            flowCheckResponse.setCurrentState(latestFlowLog.getCurrentState());
            flowCheckResponse.setNextEvent(latestFlowLog.getNextEvent());
            if (latestFlowLog.getFlowType() != null) {
                flowCheckResponse.setFlowType(latestFlowLog.getFlowType().getName());
            }
        }
    }

    private boolean completed(String marker, String flowChainId, List<FlowChainLog> flowChainLogs, List<FlowLogWithoutPayload> flowLogs) {
        if (firstIsPending(flowLogs)) {
            return false;
        }
        if (isFlowInFailedState(flowLogs, failHandledEvents)) {
            return true;
        }
        return hasFinishedFlow(marker, flowChainId, flowChainLogs, flowLogs);
    }

    private boolean hasFinishedFlow(String marker, String flowChainId, List<FlowChainLog> flowChainLogs, List<FlowLogWithoutPayload> flowLogs) {
        boolean hasFinishedFlowLog = false;
        for (FlowLogWithoutPayload flowLog : flowLogs) {
            String currentState = flowLog.getCurrentState();
            if (failHandledEvents.contains(flowLog.getNextEvent())
                    || currentState != null && CANCELLED_TERMINATED_STATES.contains(currentState)) {
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

    private boolean firstIsPending(List<FlowLogWithoutPayload> flowLogs) {
        return !flowLogs.isEmpty() && isPending(flowLogs.get(0));
    }

    private boolean isPending(FlowLogWithoutPayload flowLog) {
        return !Boolean.TRUE.equals(flowLog.getFinalized()) || StateStatus.PENDING.equals(flowLog.getStateStatus());
    }

    public Page<FlowLogResponse> getFlowLogsByIds(List<String> flowIds, Pageable pageable) {
        LOGGER.info("Getting flow logs by flow ids list {}", flowIds);
        Page<FlowLog> flowLogResponses = flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(new HashSet<>(flowIds), pageable);
        return new PageImpl<>(flowLogResponses.stream().map(flowLog -> flowLogConverter.convert(flowLog)).collect(toList()),
                flowLogResponses.getPageable(), flowLogResponses.getTotalElements());
    }

    public Page<FlowCheckResponse> getFlowChainsByChainIds(List<String> chainIds, Pageable pageable) {
        LOGGER.info("Getting flow check responses by flow check ids list {}", chainIds);
        Page<FlowChainLog> flowChainLogs = flowChainLogService
                .findAllByFlowChainIdInOrderByCreatedDesc(new HashSet<>(chainIds), pageable);
        Map<String, FlowChainLog> flowChainsMap = flowChainLogs
                .stream().filter(flowChainLog -> flowChainLog.getParentFlowChainId() == null)
                .collect(Collectors.toMap(FlowChainLog::getFlowChainId, Function.identity()));

        List<FlowChainLog> allRelatedFlowChainsList = new ArrayList<>();
        Map<String, String> parentChainIdByChainIdMap = new HashMap<>();
        Benchmark.measure(() -> flowChainsMap.forEach((parentFlowChainId, parentFlowChainLog) -> {
            List<FlowChainLog> relatedFlowChainLogs = flowChainLogService.getRelatedFlowChainLogs(List.of(parentFlowChainLog));
            relatedFlowChainLogs.forEach(log -> parentChainIdByChainIdMap.put(log.getFlowChainId(), parentFlowChainId));
            allRelatedFlowChainsList.addAll(relatedFlowChainLogs);
            allRelatedFlowChainsList.add(parentFlowChainLog);
        }), LOGGER, "Finding all related flow chains took {}ms");

        Set<String> flowChainIdsSet = allRelatedFlowChainsList.stream().map(FlowChainLog::getFlowChainId).collect(toSet());

        Map<String, List<FlowLogWithoutPayload>> flowLogsByParentChainId = fetchFlowLogsByParentChainId(flowChainIdsSet,
                parentChainIdByChainIdMap);

        List<FlowCheckResponse> result = flowChainsMap.keySet().stream()
                .map(parentFlowChainId -> convertFlowChainToFlowCheckResponse(flowChainsMap.get(parentFlowChainId),
                        flowLogsByParentChainId.getOrDefault(parentFlowChainId, List.of())))
                .collect(toList());

        return new PageImpl<>(result, flowChainLogs.getPageable(), flowChainLogs.getTotalElements());
    }

    private Map<String, List<FlowLogWithoutPayload>> fetchFlowLogsByParentChainId(Set<String> flowChainIdsSet,
            Map<String, String> parentChainIdByChainIdMap) {
        Map<String, List<FlowLogWithoutPayload>> flowLogsByChainId = flowLogDBService
                .getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(flowChainIdsSet)
                .stream().collect(groupingBy(FlowLogWithoutPayload::getFlowChainId, LinkedHashMap::new, toList()));

        Map<String, List<FlowLogWithoutPayload>> flowLogsByParentChainId = new HashMap<>();
        flowLogsByChainId.forEach((flowChainId, flowLogsList) -> {
            String parentId = parentChainIdByChainIdMap.getOrDefault(flowChainId, flowChainId);
            List<FlowLogWithoutPayload> flowLogs = flowLogsByParentChainId.getOrDefault(parentId, Lists.newArrayList());
            flowLogs.addAll(flowLogsList);
            flowLogsByParentChainId.put(parentId, flowLogs);
        });

        return flowLogsByParentChainId;
    }

    private FlowCheckResponse convertFlowChainToFlowCheckResponse(FlowChainLog chainLog, List<FlowLogWithoutPayload> relatedFlowLogs) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setFlowChainId(chainLog.getFlowChainId());
        flowCheckResponse.setHasActiveFlow(!completed(FlowConstants.FLOW_CHAIN, chainLog.getFlowChainId(), List.of(chainLog),
                relatedFlowLogs));
        flowCheckResponse.setLatestFlowFinalizedAndFailed(isFlowInFailedState(relatedFlowLogs, failHandledEvents));
        setEndTimeOnFlowCheckResponse(flowCheckResponse, relatedFlowLogs);
        flowCheckResponse.setReason(getLatestFlowReason(relatedFlowLogs));
        return flowCheckResponse;
    }
}