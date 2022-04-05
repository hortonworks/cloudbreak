package com.sequenceiq.consumption.configuration.flow;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowLog;

@Transactional(Transactional.TxType.REQUIRED)
public interface ConsumptionFlowLogRepository extends CrudRepository<FlowLog, Long> {

    @Modifying
    @Query("DELETE FROM FlowLog fl")
    int purgeArchivedEnvironmentLogs();

    @Query("SELECT DISTINCT fl.resourceId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' "
            + "AND fl.cloudbreakNodeId = :nodeId")
    Set<Long> findPendingResourcesByNodeId(@Param("nodeId") String nodeId);
}
