package com.sequenceiq.cloudbreak.core.flow2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.service.TransactionService;

public interface FlowLogService {
    FlowLog save(String flowId, String flowChanId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState);

    Iterable<FlowLog> saveAll(Iterable<FlowLog> entities);

    FlowLog close(Long stackId, String flowId) throws TransactionService.TransactionExecutionException;

    FlowLog cancel(Long stackId, String flowId) throws TransactionService.TransactionExecutionException;

    FlowLog terminate(Long stackId, String flowId) throws TransactionService.TransactionExecutionException;

    void purgeTerminatedStacksFlowLogs() throws TransactionService.TransactionExecutionException;

    void saveChain(String flowChainId, String parentFlowChainId, Queue<Selectable> chain);

    void updateLastFlowLogStatus(FlowLog lastFlowLog, boolean failureEvent);

    boolean isOtherFlowRunning(Long stackId);

    boolean repeatedFlowState(FlowLog lastFlowLog, String event);

    void updateLastFlowLogPayload(FlowLog lastFlowLog, Payload payload, Map<Object, Object> variables);

    Optional<FlowLog> getLastFlowLog(String flowId);

    Set<String> findAllRunningNonTerminationFlowIdsByStackId(Long stackId);

    Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId);

    Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    List<Object[]> findAllPending();

    Set<FlowLog> findAllUnassigned();

    Set<FlowLog> findAllByCloudbreakNodeId(String cloudbreakNodeId);

    List<FlowLog> findAllByStackIdOrderByCreatedDesc(Long id);

    Set<Long> findTerminatingStacksByCloudbreakNodeId(String id);

}
