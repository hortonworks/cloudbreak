package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.FlowChainLog;

@EntityType(entityClass = FlowChainLog.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface FlowChainLogRepository extends CrudRepository<FlowChainLog, Long> {

    FlowChainLog findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    @Modifying
    @Query("DELETE FROM FlowChainLog fch WHERE fch.flowChainId NOT IN ( SELECT DISTINCT fl.flowChainId FROM FlowLog fl )")
    int purgeOrphanFLowChainLogs();
}
