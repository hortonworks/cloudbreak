package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AwsCredential;

public interface AwsCredentialRepository extends CrudRepository<AwsCredential, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AwsCredential findOne(Long id);
}