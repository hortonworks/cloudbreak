package com.sequenceiq.flow.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;

public interface FlowLogService {
    FlowLog save(FlowEventContext flowEventContext, Map<Object, Object> variables, Class<?> flowType, FlowState currentState);

    Iterable<FlowLog> saveAll(Iterable<FlowLog> entities);

    FlowLog finish(FlowEventContext flowEventContext, Map<Object, Object> contextParams, boolean failed, String reason) throws TransactionExecutionException;

    FlowLog cancel(Long resourceId, String flowId) throws TransactionExecutionException;

    FlowLog terminate(Long resourceId, String flowId, String reason) throws TransactionExecutionException;

    void saveChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue chain, String flowTriggerUserCrn);

    void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent, String reason);

    Set<FlowLogIdWithTypeAndTimestamp> findAllRunningFlowsByResourceId(Long resourceId);

    boolean isOtherFlowRunning(Long resourceId);

    boolean repeatedFlowState(FlowLog lastFlowLog, String event);

    void updateLastFlowLogPayload(FlowLog lastFlowLog, Payload payload, Map<Object, Object> variables);

    Optional<FlowLogWithoutPayload> getLastFlowLog(String flowId);

    List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId);

    void cancelTooOldTerminationFlowForResource(Long resourceId, long olderThan);

    Set<String> findAllRunningNonTerminationFlowIdsByStackId(Long resourceId);

    Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId);

    Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    List<Object[]> findAllPending();

    Set<FlowLog> findAllUnassigned();

    Set<FlowLog> findAllByCloudbreakNodeId(String cloudbreakNodeId);

    List<FlowLog> findAllForLastFlowIdByResourceIdOrderByCreatedDesc(Long id);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long id);

    Optional<FlowLog> getLastFlowLog(Long resourceId);

    Optional<FlowLog> getLastFlowLogWithEndTime(Long resourceId);

    boolean isFlowConfigAlreadyRunning(Long id, Class<? extends FlowConfiguration<?>> flowConfiguration);

    List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long id);

    int purgeFinalizedSuccessfulFlowLogs(int retentionPeriodHours);

    int purgeFinalizedFailedFlowLogs(int retentionPeriodHours);

    List<FlowLogWithoutPayload> findAllWithoutPayloadByFlowIdOrderByCreatedDesc(String flowId);

    List<FlowLogWithoutPayload> getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set<String> relatedFlowIds);

    List<FlowLog> findAllFlowByFlowChainId(Set<String> chainIds);
}
