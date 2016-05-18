package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.FlowChainLog;

@EntityType(entityClass = FlowChainLog.class)
public interface FlowChainLogRepository extends CrudRepository<FlowChainLog, Long> {

    FlowChainLog findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);
}
