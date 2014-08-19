package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.model.ScalingPolicy;

public interface ScalingPolicyRepository extends CrudRepository<ScalingPolicy, Long> {
}
