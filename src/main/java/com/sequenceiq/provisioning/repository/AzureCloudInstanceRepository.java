package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.AzureCloudInstance;

public interface AzureCloudInstanceRepository extends CrudRepository<AzureCloudInstance, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    AzureCloudInstance findOne(Long id);
}