package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.CredentialHistory;
import org.springframework.data.repository.CrudRepository;

public interface CredentialHistoryRepository extends CrudRepository<CredentialHistory, Long> {
}
