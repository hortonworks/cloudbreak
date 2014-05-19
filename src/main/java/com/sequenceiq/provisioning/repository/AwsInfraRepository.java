package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.User;

public interface AwsInfraRepository extends CrudRepository<AwsInfra, Long> {

    User findByName(String name);

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AwsInfra findOne(Long id);
}