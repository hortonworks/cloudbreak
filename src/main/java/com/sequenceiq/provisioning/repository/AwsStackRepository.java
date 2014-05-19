package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AwsStack;
import com.sequenceiq.provisioning.domain.User;

public interface AwsStackRepository extends CrudRepository<AwsStack, Long> {

    User findByName(String name);
}