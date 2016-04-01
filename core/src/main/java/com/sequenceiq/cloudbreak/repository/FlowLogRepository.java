package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.FlowLog;

@EntityType(entityClass = FlowLog.class)
public interface FlowLogRepository extends CrudRepository<FlowLog, Long> {
}
