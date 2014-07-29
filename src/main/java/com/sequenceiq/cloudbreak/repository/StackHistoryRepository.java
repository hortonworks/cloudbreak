package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.StackHistory;

public interface StackHistoryRepository extends CrudRepository<StackHistory, Long> {

}
