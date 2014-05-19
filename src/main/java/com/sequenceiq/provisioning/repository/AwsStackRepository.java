package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.User;

public interface AwsStackRepository extends CrudRepository<AwsInfra, Long> {

    User findByName(String name);
}