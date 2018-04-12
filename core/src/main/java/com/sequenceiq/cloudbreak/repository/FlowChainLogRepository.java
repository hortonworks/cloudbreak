package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = FlowChainLog.class)
public interface FlowChainLogRepository extends CrudRepository<FlowChainLog, Long> {

    FlowChainLog findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    @Modifying
    @Query("DELETE FROM FlowChainLog fch WHERE fch.flowChainId NOT IN ( SELECT DISTINCT fl.flowChainId FROM FlowLog fl )")
    int purgeOrphanFLowChainLogs();
}
