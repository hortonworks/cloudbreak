package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.User;

public interface AzureTemplateRepository extends CrudRepository<AzureTemplate, Long> {

    User findByName(String name);

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AzureTemplate findOne(@Param("id") Long id);
}