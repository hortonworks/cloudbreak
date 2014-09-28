package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.GccCredential;

public interface GccCredentialRepository extends CrudRepository<GccCredential, Long> {

    @PostAuthorize("returnObject?.owner?.id == principal?.id")
    GccCredential findOne(Long id);
}
