package com.sequenceiq.redbeams.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
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

    @Query("SELECT c.resourceCrn FROM DatabaseConfig c WHERE c.accountId = :accountId")
    List<Crn> findAllResourceCrnsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT c.resourceCrn FROM DatabaseConfig c WHERE c.name = :name")
    Optional<Crn> findResourceCrnByName(@Param("name") String name);

    @Query("SELECT c.name FROM DatabaseConfig c WHERE c.resourceCrn = :resourceCrn")
    Optional<String> findNameByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT c.name as name, c.resourceCrn as crn FROM DatabaseConfig c WHERE c.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrn(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT c.resourceCrn FROM DatabaseConfig c WHERE c.name IN (:names)")
    List<Crn> findResourceCrnsByNames(@Param("names") Collection<String> names);
}
