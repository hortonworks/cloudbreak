package com.sequenceiq.environment.environment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Environment.class)
public interface EnvironmentRepository extends AccountAwareResourceRepository<Environment, Long> {

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "WHERE e.accountId = :accountId "
            + "AND e.archived = false")
    Set<Environment> findByAccountId(@Param("accountId") String accountId);

    Set<Environment> findByNameInAndAccountIdAndArchivedIsFalse(Collection<String> names, String accountId);

    Set<Environment> findByResourceCrnInAndAccountIdAndArchivedIsFalse(Collection<String> resourceCrns, String accountId);

    Optional<Environment> findByNameAndAccountIdAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    Optional<Environment> findByResourceCrnAndAccountIdAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT COUNT(e)>0 FROM Environment e WHERE e.name = :name AND e.accountId = :accountId AND e.archived = false")
    boolean existsWithNameAndAccountAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    List<Environment> findAllByIdInAndStatusInAndArchivedIsFalse(Collection<Long> ids, Collection<EnvironmentStatus> statuses);

    List<Environment> findAllByStatusInAndArchivedIsFalse(Collection<EnvironmentStatus> statuses);

    @Query("SELECT e.resourceCrn FROM Environment e WHERE e.name = :name AND e.accountId = :accountId")
    Optional<String> findResourceCrnByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.name FROM Environment e WHERE e.resourceCrn = :resourceCrn AND e.accountId = :accountId")
    Optional<String> findNameByResourceCrnAndAccountId(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT e.name as name, e.resourceCrn as crn FROM Environment e WHERE e.accountId = :accountId AND e.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    @Query("SELECT e.name FROM Environment e "
            + "JOIN e.parentEnvironment pe "
            + "WHERE pe.id = :parentEnvironmentId AND e.accountId = :accountId AND e.archived = false")
    List<String> findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(@Param("accountId") String accountId,
        @Param("parentEnvironmentId") Long parentEnvironmentId);

    @Query("SELECT e FROM Environment e "
            + "JOIN e.parentEnvironment pe "
            + "WHERE pe.id = :parentEnvironmentId AND e.accountId = :accountId AND e.archived = false")
    List<Environment> findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(@Param("accountId") String accountId,
            @Param("parentEnvironmentId") Long parentEnvironmentId);

    @Query("SELECT e FROM Environment e WHERE e.archived = false and e.status in (:statuses)")
    List<Environment> findAllRunningAndStatusIn(@Param("statuses") Collection<EnvironmentStatus> statuses);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(e.id, e.resourceCrn) FROM Environment e " +
            "WHERE e.accountId = :accountId AND e.archived = false")
    List<ResourceWithId> findAsAuthorizationResourcesInAccount(@Param("accountId") String accountId);
}
