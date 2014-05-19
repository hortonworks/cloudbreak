package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AzureCloudInstance;

public interface AzureCloudInstanceRepository extends CrudRepository<AzureCloudInstance, Long> {

}