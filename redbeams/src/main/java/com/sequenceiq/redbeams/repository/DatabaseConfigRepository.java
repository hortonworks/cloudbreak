package com.sequenceiq.redbeams.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.RedbeamsResourceCrnAndNameView;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfig, Long> {

    Optional<DatabaseConfig> findByEnvironmentIdAndName(String environmentId, String name);

    Optional<DatabaseConfig> findByName(String name);

    Optional<DatabaseConfig> findByResourceCrn(Crn crn);

    Set<DatabaseConfig> findByEnvironmentId(String environmentId);

    Set<DatabaseConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    @Query("SELECT c.resourceCrn FROM DatabaseConfig c WHERE c.name = :name")
    Optional<Crn> findResourceCrnByName(@Param("name") String name);

    List<RedbeamsResourceCrnAndNameView> findByResourceCrnIn(Collection<Crn> resourceCrns);

    @Query("SELECT c.resourceCrn FROM DatabaseConfig c WHERE c.name IN (:names)")
    List<Crn> findResourceCrnsByNames(@Param("names") Collection<String> names);
}
