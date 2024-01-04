package com.sequenceiq.environment.environment.flow;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowLog;

@Transactional(Transactional.TxType.REQUIRED)
public interface EnvironmentFlowLogRepository extends CrudRepository<FlowLog, Long> {

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.resourceId IN (SELECT e.id FROM Environment e WHERE e.status = 'ARCHIVED')")
    int purgeArchivedEnvironmentLogs();

    @Query("SELECT DISTINCT fl.resourceId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' "
            + "AND fl.cloudbreakNodeId = :nodeId "
            + "AND fl.flowType = 'com.sequenceiq.environment.environment.flow.delete.config.EnvDeleteFlowConfig'")
    Set<Long> findPendingResourcesByNodeId(@Param("nodeId") String nodeId);
}
