package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Template;

public interface TemplateRepository extends CrudRepository<Template, Long> {

    @PostAuthorize("returnObject?.awsTemplateOwner?.id == principal?.id || returnObject?.azureTemplateOwner?.id == principal?.id")
    Template findOne(Long id);
}