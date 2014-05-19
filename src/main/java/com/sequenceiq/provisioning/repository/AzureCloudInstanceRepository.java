package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.AwsCloudInstance;

public interface AzureCloudInstanceRepository extends CrudRepository<AwsCloudInstance, Long> {

}