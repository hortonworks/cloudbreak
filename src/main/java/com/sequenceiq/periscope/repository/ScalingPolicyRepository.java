package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.ScalingPolicy;

public interface ScalingPolicyRepository extends CrudRepository<ScalingPolicy, Long> {
}
