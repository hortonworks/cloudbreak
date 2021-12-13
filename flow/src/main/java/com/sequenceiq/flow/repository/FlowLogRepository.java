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

import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.StateStatus;

@Transactional(TxType.REQUIRED)
public interface FlowLogRepository extends CrudRepository<FlowLog, Long> {

    Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId);

    @Query("SELECT fl.flowId as flowId, fl.flowType as flowType, fl.created as created FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' AND fl.resourceId = :resourceId")
    Set<FlowLogIdWithTypeAndTimestamp> findAllRunningFlowLogByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT DISTINCT fl.flowId, fl.resourceId, fl.cloudbreakNodeId FROM FlowLog fl WHERE fl.stateStatus = 'PENDING'")
    List<Object[]> findAllPending();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.finalized = true WHERE fl.flowId = :flowId")
    void finalizeByFlowId(@Param("flowId") String flowId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId = :cloudbreakNodeId AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId IS NULL AND fl.stateStatus = 'PENDING'")
    Set<FlowLog> findAllUnassigned();

    @Query("SELECT fl FROM FlowLog fl WHERE fl.flowType = :flowType AND fl.resourceId = :resourceId ORDER BY fl.created DESC")
    List<FlowLog> findAllFlowByType(@Param("resourceId") Long resourceId, @Param("flowType") ClassValue classValue);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.created IN " +
            "( SELECT max(fls.created) from FlowLog fls WHERE fls.flowType = :flowType and fls.resourceId = :resourceId " +
            "GROUP BY (fls.flowId, fls.resourceId)) " +
            "ORDER BY fl.created DESC ")
    List<FlowLog> findLastFlowLogsByTypeAndResourceId(@Param("resourceId") Long resourceId, @Param("flowType") ClassValue classValue);

    @Query("SELECT fl.flowId FROM FlowLog fl WHERE fl.flowChainId IN (:chainIds)")
    Set<String> findAllFlowIdsByChainIds(@Param("chainIds") Set<String> chainIds);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.flowId IN (:flowIds) ORDER BY fl.created DESC")
    List<FlowLog> findAllByFlowIdsCreatedDesc(@Param("flowIds") Set<String> flowIds);

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.stateStatus = :stateStatus WHERE fl.id = :id")
    void updateLastLogStatusInFlow(@Param("id") Long id, @Param("stateStatus") StateStatus stateStatus);

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.finalized = TRUE")
    int purgeFinalizedFlowLogs();

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId);

    Optional<FlowLog> findFirstByResourceIdOrderByCreatedDesc(@Param("resourceId") Long resourceId);

    List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long resourceId);

    List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId);

    List<FlowLog> findAllByFlowChainIdOrderByCreatedDesc(String flowChainId);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId, Pageable page);

    @Query("SELECT COUNT(fl.id) > 0 FROM FlowLog fl WHERE fl.resourceId = :resourceId AND fl.stateStatus = :status")
    Boolean findAnyByStackIdAndStateStatus(@Param("resourceId") Long resourceId, @Param("status") StateStatus status);
}
