package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.Credential;

public interface CredentialRepository extends CrudRepository<Credential, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Credential findOne(Long id);
}