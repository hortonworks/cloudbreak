package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.ClusterHistory;
import org.springframework.data.repository.CrudRepository;

public interface ClusterHistoryRepository extends CrudRepository<ClusterHistory, Long> {
}
