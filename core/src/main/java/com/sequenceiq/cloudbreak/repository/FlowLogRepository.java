package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = FlowLog.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface FlowLogRepository extends DisabledBaseRepository<FlowLog, Long> {

    FlowLog findFirstByFlowIdOrderByCreatedDesc(String flowId);

    @Query("SELECT DISTINCT fl.flowId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' AND fl.stackId = :stackId "
            + "AND fl.flowType != 'com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig'")
    Set<String> findAllRunningNonTerminationFlowIdsByStackId(@Param("stackId") Long stackId);

    @Query("SELECT DISTINCT fl.stackId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' "
            + "AND fl.cloudbreakNodeId = :cloudbreakNodeId "
            + "AND fl.flowType = 'com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig'")
    Set<Long> findTerminatingStacksByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

    @Query("SELECT DISTINCT fl.flowId, fl.stackId, fl.cloudbreakNodeId FROM FlowLog fl WHERE fl.stateStatus = 'PENDING'")
    List<Object[]> findAllPending();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.finalized = true WHERE fl.flowId = :flowId")
    void finalizeByFlowId(@Param("flowId") String flowId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId = :cloudbreakNodeId AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId IS NULL AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllUnassigned();

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.finalized = TRUE")
    int purgeFinalizedFlowLogs();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.stateStatus = :stateStatus WHERE fl.id = :id")
    void updateLastLogStatusInFlow(@Param("id") Long id, @Param("stateStatus") StateStatus stateStatus);

    List<FlowLog> findAllByStackIdOrderByCreatedDesc(Long stackId, Pageable page);

    @Query("SELECT COUNT(fl.id) > 0 FROM FlowLog fl WHERE fl.stackId = :stackId AND fl.stateStatus = :status")
    Boolean findAnyByStackIdAndStateStatus(@Param("stackId") Long stackId, @Param("status") StateStatus status);
}
