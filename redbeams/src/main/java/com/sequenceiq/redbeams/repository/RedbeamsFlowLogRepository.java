package com.sequenceiq.redbeams.repository;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = FlowLog.class)
public interface RedbeamsFlowLogRepository extends FlowLogRepository {

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.resourceId NOT IN (SELECT d.id FROM DBStack d)")
    int purgeDeletedDbStacksLogs();

    @Query("SELECT DISTINCT fl.resourceId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' "
            + "AND fl.cloudbreakNodeId = :nodeId "
            + "AND fl.flowType = 'com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationFlowConfig'")
    Set<Long> findTerminatingResourcesByNodeId(@Param("nodeId") String nodeId);
}
