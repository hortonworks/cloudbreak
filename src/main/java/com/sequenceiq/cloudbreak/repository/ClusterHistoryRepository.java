package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.ClusterHistory;

public interface ClusterHistoryRepository extends CrudRepository<ClusterHistory, Long> {
}
