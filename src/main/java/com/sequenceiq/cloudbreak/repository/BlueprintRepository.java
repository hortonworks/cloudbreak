package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Blueprint;

public interface BlueprintRepository extends CrudRepository<Blueprint, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Blueprint findOne(@Param("id") Long id);
}