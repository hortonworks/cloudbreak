package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.PollingData;

public interface PollingDataRepository extends CrudRepository<PollingData, Long> {

    PollingData findByStackId(Long stackId);

}