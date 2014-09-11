package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.AwsCredential;

public interface AwsCredentialRepository extends CrudRepository<AwsCredential, Long> {

    @PostAuthorize("returnObject?.owner == principal")
    AwsCredential findOne(@Param("id") Long id);
}