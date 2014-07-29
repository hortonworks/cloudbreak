package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CredentialHistory;

public interface CredentialHistoryRepository extends CrudRepository<CredentialHistory, Long> {
}
