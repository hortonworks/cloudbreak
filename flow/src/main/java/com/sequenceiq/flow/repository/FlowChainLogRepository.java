package com.sequenceiq.flow.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.flow.domain.FlowChainLog;

@Transactional(TxType.REQUIRED)
public interface FlowChainLogRepository extends CrudRepository<FlowChainLog, Long> {

    List<FlowChainLog> findByParentFlowChainIdOrderByCreatedDesc(String parentFlowChainId);

    List<FlowChainLog> findByFlowChainIdOrderByCreatedDesc(String flowChainId);

    Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    @Modifying
    @Query("DELETE FROM FlowChainLog fch WHERE fch.flowChainId NOT IN ( SELECT DISTINCT fl.flowChainId FROM FlowLog fl )")
    int purgeOrphanFLowChainLogs();
}
