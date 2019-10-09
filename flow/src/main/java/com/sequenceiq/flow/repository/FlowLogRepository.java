package com.sequenceiq.flow.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdFlowAndType;
import com.sequenceiq.flow.domain.StateStatus;

@Transactional(TxType.REQUIRED)
public interface FlowLogRepository extends CrudRepository<FlowLog, Long> {

    Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId);

    @Query("SELECT fl.flowId as flowId, fl.flowType as flowType FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' AND fl.resourceId = :resourceId")
    Set<FlowLogIdFlowAndType> findAllRunningFlowLogByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT DISTINCT fl.flowId, fl.resourceId, fl.cloudbreakNodeId FROM FlowLog fl WHERE fl.stateStatus = 'PENDING'")
    List<Object[]> findAllPending();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.finalized = true WHERE fl.flowId = :flowId")
    void finalizeByFlowId(@Param("flowId") String flowId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId = :cloudbreakNodeId AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId IS NULL AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllUnassigned();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.stateStatus = :stateStatus WHERE fl.id = :id")
    void updateLastLogStatusInFlow(@Param("id") Long id, @Param("stateStatus") StateStatus stateStatus);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId);

    List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId, Pageable page);

    @Query("SELECT COUNT(fl.id) > 0 FROM FlowLog fl WHERE fl.resourceId = :resourceId AND fl.stateStatus = :status")
    Boolean findAnyByStackIdAndStateStatus(@Param("resourceId") Long resourceId, @Param("status") StateStatus status);
}
