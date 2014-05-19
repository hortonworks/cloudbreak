package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AwsCloudInstance;

public interface AwsCloudInstanceRepository extends CrudRepository<AwsCloudInstance, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AwsCloudInstance findOne(Long id);
}