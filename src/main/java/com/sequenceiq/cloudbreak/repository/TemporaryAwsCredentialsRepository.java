package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.TemporaryAwsCredentials;

public interface TemporaryAwsCredentialsRepository extends CrudRepository<TemporaryAwsCredentials, Long> {

}
