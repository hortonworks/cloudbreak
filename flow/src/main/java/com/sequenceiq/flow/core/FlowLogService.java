package com.sequenceiq.flow.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

public interface FlowLogService {
    FlowLog save(FlowParameters flowParameters, String flowChanId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState);

    Iterable<FlowLog> saveAll(Iterable<FlowLog> entities);

    FlowLog close(Long resourceId, String flowId, boolean failed) throws TransactionService.TransactionExecutionException;

    FlowLog cancel(Long resourceId, String flowId) throws TransactionService.TransactionExecutionException;

    FlowLog terminate(Long resourceId, String flowId) throws TransactionService.TransactionExecutionException;

    void saveChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue chain, String flowTriggerUserCrn);

    void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent);

    Set<FlowLogIdWithTypeAndTimestamp> findAllRunningNonTerminationFlowsByResourceId(Long resourceId);

    boolean isOtherNonTerminationFlowRunning(Long resourceId);

    boolean isOtherFlowRunning(Long resourceId);

    boolean repeatedFlowState(FlowLog lastFlowLog, String event);

    void updateLastFlowLogPayload(FlowLog lastFlowLog, Payload payload, Map<Object, Object> variables);

    Optional<FlowLog> getLastFlowLog(String flowId);

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

    List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long id);

    int purgeFinalizedFlowLogs();
}
