package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.User;

public interface AwsTemplateRepository extends CrudRepository<AwsTemplate, Long> {

    User findByName(String name);

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AwsTemplate findOne(Long id);
}