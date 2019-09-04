package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfig, Long> {

    Optional<DatabaseConfig> findByEnvironmentIdAndName(String environmentId, String name);

    Optional<DatabaseConfig> findByName(String name);

    Optional<DatabaseConfig> findByResourceCrn(Crn crn);

    Set<DatabaseConfig> findByEnvironmentId(String environmentId);

    Set<DatabaseConfig> findByResourceCrnIn(Set<Crn> resourceCrns);
}
