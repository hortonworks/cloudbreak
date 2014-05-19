package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.User;

public interface AzureInfraRepository extends CrudRepository<AzureInfra, Long> {

    User findByName(String name);
}