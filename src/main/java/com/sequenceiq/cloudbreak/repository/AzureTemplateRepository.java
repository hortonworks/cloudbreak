package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.AzureTemplate;

public interface AzureTemplateRepository extends CrudRepository<AzureTemplate, Long> {

    @PostAuthorize("returnObject?.user == principal")
    AzureTemplate findOne(@Param("id") Long id);
}