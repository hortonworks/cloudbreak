package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.TemporaryAwsCredentials;

public interface TemporaryAwsCredentialsRepository extends CrudRepository<TemporaryAwsCredentials, Long> {

}
