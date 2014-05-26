package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.Blueprint;

public interface BlueprintRepository extends CrudRepository<Blueprint, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Blueprint findOne(Long id);
}