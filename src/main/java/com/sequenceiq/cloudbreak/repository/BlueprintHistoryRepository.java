package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.BlueprintHistory;

public interface BlueprintHistoryRepository extends CrudRepository<BlueprintHistory, Long> {

}
