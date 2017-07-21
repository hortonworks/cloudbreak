package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.FlowLog;

@EntityType(entityClass = FlowLog.class)
public interface FlowLogRepository extends CrudRepository<FlowLog, Long> {

    FlowLog findFirstByFlowIdOrderByCreatedDesc(String flowId);

    @Query("SELECT DISTINCT fl.flowId FROM FlowLog fl "
            + "WHERE (fl.finalized IS NULL OR fl.finalized = false) AND fl.stackId = :stackId "
            + "AND fl.flowType != 'com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig'")
    Set<String> findAllRunningNonTerminationFlowIdsByStackId(@Param("stackId") Long stackId);

    @Query("SELECT DISTINCT fl.flowId, fl.stackId, fl.cloudbreakNodeId FROM FlowLog fl WHERE fl.finalized IS NULL OR fl.finalized = false")
    List<Object[]> findAllNonFinalized();

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.finalized = true WHERE fl.flowId = :flowId")
    void finalizeByFlowId(@Param("flowId") String flowId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId = :cloudbreakNodeId AND (fl.finalized IS NULL OR fl.finalized = false)")
    Set<FlowLog> findAllByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

    @Query("SELECT fl FROM FlowLog fl WHERE fl.cloudbreakNodeId IS NULL AND (fl.finalized IS NULL OR fl.finalized = false)")
    Set<FlowLog> findAllUnassigned();

}
