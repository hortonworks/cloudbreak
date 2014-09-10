package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsageRepository extends CrudRepository<CloudbreakUsage, Long>, JpaSpecificationExecutor {
}
