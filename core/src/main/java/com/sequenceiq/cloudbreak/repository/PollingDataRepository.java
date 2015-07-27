package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.PollingData;

@EntityType(entityClass = PollingData.class)
public interface PollingDataRepository extends CrudRepository<PollingData, Long> {

    PollingData findByStackId(Long stackId);

}