package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.GccTemplate;

public interface GccTemplateRepository extends CrudRepository<GccTemplate, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    GccTemplate findOne(Long id);
}
