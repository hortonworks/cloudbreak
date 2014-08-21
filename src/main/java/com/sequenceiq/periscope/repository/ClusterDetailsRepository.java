package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.ClusterDetails;

public interface ClusterDetailsRepository extends CrudRepository<ClusterDetails, Long> {
}
