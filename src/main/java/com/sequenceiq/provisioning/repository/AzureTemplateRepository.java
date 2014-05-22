package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.User;

public interface AzureTemplateRepository extends CrudRepository<AzureTemplate, Long> {

    User findByName(String name);

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AzureTemplate findOne(Long id);
}