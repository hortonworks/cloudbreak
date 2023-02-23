package com.sequenceiq.flow.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;

@Transactional(TxType.REQUIRED)
public interface FlowLogRepository extends CrudRepository<FlowLog, Long> {

    Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId);

    @Query("SELECT fl.flowId as flowId, " +
            "fl.resourceId as resourceId, " +
            "fl.created as created, " +
            "fl.flowChainId as flowChainId, " +
            "fl.nextEvent as nextEvent, " +
            "fl.payloadType as payloadType, " +
            "fl.flowType as flowType, " +
            "fl.currentState as currentState, " +
            "fl.finalized as finalized, " +
            "fl.cloudbreakNodeId as cloudbreakNodeId, " +
            "fl.stateStatus as stateStatus, " +
            "fl.version as version, " +
            "fl.resourceType as resourceType, " +
            "fl.flowTriggerUserCrn as flowTriggerUserCrn, " +
            "fl.operationType as operationType " +
            "FROM FlowLog fl " +
            "WHERE fl.flowId = :flowId " +
            "ORDER BY fl.created desc")
    Page<FlowLogWithoutPayload> findByFlowIdOrderByCreatedDesc(@Param("flowId") String flowId, Pageable page);

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
            "AND fls.currentState <> 'FINISHED' " +
            "GROUP BY (fls.flowId, fls.resourceId)) " +
            "ORDER BY fl.created DESC ")
    List<FlowLog> findLastNotFinishedFlowLogsByTypeAndResourceId(@Param("resourceId") Long resourceId, @Param("flowType") ClassValue classValue);

    @Query("SELECT fl.flowId FROM FlowLog fl WHERE fl.flowChainId IN (:chainIds)")
    Set<String> findAllFlowIdsByChainIds(@Param("chainIds") Set<String> chainIds);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.flowId IN (:flowIds) ORDER BY fl.created DESC")
    Page<FlowLog> findAllByFlowIdsCreatedDesc(@Param("flowIds") Set<String> flowIds, Pageable pageable);

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.stateStatus = :stateStatus, fl.endTime = :endTime WHERE fl.id = :id")
    void updateLastLogStatusInFlow(@Param("id") Long id, @Param("stateStatus") StateStatus stateStatus, @Param("endTime") Long endTime);

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.finalized = TRUE AND fl.endTime <= :endTime")
    int purgeFinalizedFlowLogs(@Param("endTime") Long endTime);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId);

    Optional<FlowLog> findFirstByResourceIdOrderByCreatedDesc(@Param("resourceId") Long resourceId);

    Optional<FlowLog> findFirstByResourceIdAndEndTimeNotNullOrderByCreatedDesc(@Param("resourceId") Long resourceId);

    List<FlowLog> findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(Long resourceId);

    List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId);

    List<FlowLog> findAllByFlowChainIdOrderByCreatedDesc(String flowChainId);

    List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId, Pageable page);

    @Query("SELECT COUNT(fl.id) > 0 FROM FlowLog fl WHERE fl.resourceId = :resourceId AND fl.stateStatus = :status")
    Boolean findAnyByStackIdAndStateStatus(@Param("resourceId") Long resourceId, @Param("status") StateStatus status);

    @Query("SELECT fl.flowId as flowId, " +
            "fl.resourceId as resourceId, " +
            "fl.created as created, " +
            "fl.endTime as endTime, " +
            "fl.flowChainId as flowChainId, " +
            "fl.nextEvent as nextEvent, " +
            "fl.payloadType as payloadType, " +
            "fl.flowType as flowType, " +
            "fl.currentState as currentState, " +
            "fl.finalized as finalized, " +
            "fl.cloudbreakNodeId as cloudbreakNodeId, " +
            "fl.stateStatus as stateStatus, " +
            "fl.version as version, " +
            "fl.resourceType as resourceType, " +
            "fl.flowTriggerUserCrn as flowTriggerUserCrn, " +
            "fl.operationType as operationType " +
            "FROM FlowLog fl " +
            "WHERE fl.flowChainId IN (:chainIds) " +
            "ORDER BY fl.created DESC")
    List<FlowLogWithoutPayload> findAllWithoutPayloadByChainIdsCreatedDesc(@Param("chainIds") Set<String> chainIds);

    @Query("SELECT fl.flowId as flowId, " +
            "fl.resourceId as resourceId, " +
            "fl.created as created, " +
            "fl.flowChainId as flowChainId, " +
            "fl.nextEvent as nextEvent, " +
            "fl.payloadType as payloadType, " +
            "fl.flowType as flowType, " +
            "fl.currentState as currentState, " +
            "fl.finalized as finalized, " +
            "fl.cloudbreakNodeId as cloudbreakNodeId, " +
            "fl.stateStatus as stateStatus, " +
            "fl.version as version, " +
            "fl.resourceType as resourceType, " +
            "fl.flowTriggerUserCrn as flowTriggerUserCrn, " +
            "fl.operationType as operationType " +
            "FROM FlowLog fl " +
            "WHERE fl.flowId = :flowId " +
            "ORDER BY fl.created desc")
    List<FlowLogWithoutPayload> findAllWithoutPayloadByFlowIdOrderByCreatedDesc(@Param("flowId") String flowId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.flowChainId IN (:chainIds) AND fl.currentState <> 'FINISHED' ORDER BY fl.created DESC")
    List<FlowLog> findAllByFlowChainIdOrderByCreatedDesc(@Param("chainIds") Set<String> chainIds);
}
