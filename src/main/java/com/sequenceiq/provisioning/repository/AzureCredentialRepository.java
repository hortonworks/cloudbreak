package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AzureCredential;

public interface AzureCredentialRepository extends CrudRepository<AzureCredential, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AzureCredential findOne(Long id);
}