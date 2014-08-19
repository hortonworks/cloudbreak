package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.model.ClusterDetails;

public interface ClusterDetailsRepository extends CrudRepository<ClusterDetails, String> {
}
