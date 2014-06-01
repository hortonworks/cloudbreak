package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.Template;

public interface TemplateRepository extends CrudRepository<Template, Long> {

    @PostAuthorize("returnObject?.awsTemplateOwner?.id == principal?.id || returnObject?.azureTemplateOwner?.id == principal?.id")
    Template findOne(Long id);
}