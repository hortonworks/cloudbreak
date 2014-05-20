package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.Infra;

public interface CloudInstanceRepository extends CrudRepository<Infra, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Infra findOne(Long id);
}