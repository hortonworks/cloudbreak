package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowLog;

@Transactional(Transactional.TxType.REQUIRED)
public interface CloudbreakFlowLogRepository extends Repository<FlowLog, Long> {

    @Modifying
    @Query("DELETE FROM FlowLog fl WHERE fl.resourceId IN ( SELECT st.id FROM Stack st WHERE st.stackStatus.status = 'DELETE_COMPLETED')")
    int purgeTerminatedStackLogs();

    @Query("SELECT DISTINCT fl.resourceId FROM FlowLog fl "
            + "WHERE fl.stateStatus = 'PENDING' "
            + "AND fl.cloudbreakNodeId = :cloudbreakNodeId "
            + "AND fl.flowType = 'com.sequenceiq.flow.core.stack.termination.StackTerminationFlowConfig'")
    Set<Long> findTerminatingStacksByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);

}
