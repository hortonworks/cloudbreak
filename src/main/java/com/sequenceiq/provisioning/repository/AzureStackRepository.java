package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AzureStack;
import com.sequenceiq.provisioning.domain.User;

public interface AzureStackRepository extends CrudRepository<AzureStack, Long> {

    User findByName(String name);
}