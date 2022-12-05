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
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Environment.class)
public interface EnvironmentRepository extends AccountAwareResourceRepository<Environment, Long>, JobResourceRepository<Environment, Long> {

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "WHERE e.accountId = :accountId "
            + "AND e.archived = false")
    Set<Environment> findByAccountId(@Param("accountId") String accountId);

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "WHERE e.id IN :ids "
            + "AND e.archived = false")
    Set<Environment> findAllByIdNotArchived(@Param("ids") List<Long> ids);

    @Query("SELECT e.resourceCrn FROM Environment e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId " +
            "AND e.archived = false")
    List<String> findAllCrnByNameInAndAccountIdAndArchivedIsFalse(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "WHERE e.accountId = :accountId "
            + "AND e.name = :name "
            + "AND e.archived = false")
    Optional<Environment> findByNameAndAccountIdAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "LEFT JOIN FETCH e.proxyConfig pc "
            + "LEFT JOIN FETCH e.parentEnvironment pe "
            + "WHERE e.accountId = :accountId "
            + "AND e.resourceCrn = :resourceCrn "
            + "AND e.archived = false")
    Optional<Environment> findByResourceCrnAndAccountIdAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT e.id FROM Environment e " +
            "WHERE e.accountId = :accountId " +
            "AND e.name = :name " +
            "AND e.archived = false")
    Optional<Long> findIdByNameAndAccountIdAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.id FROM Environment e " +
            "WHERE e.accountId = :accountId " +
            "AND e.resourceCrn = :resourceCrn " +
            "AND e.archived = false")
    Optional<Long> findIdByResourceCrnAndAccountIdAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

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

    @Query("SELECT e FROM Environment e "
            + "JOIN e.parentEnvironment pe "
            + "WHERE pe.id = :parentEnvironmentId AND e.accountId = :accountId AND e.archived = false")
    List<Environment> findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(@Param("accountId") String accountId,
            @Param("parentEnvironmentId") Long parentEnvironmentId);

    @Query("SELECT e.resourceCrn as remoteResourceId, e.id as localId, e.name as name FROM Environment e WHERE e.archived = false " +
            "and e.status not in (:statuses)")
    List<JobResource> findAllRunningAndStatusNotIn(@Param("statuses") Collection<EnvironmentStatus> statuses);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(e.id, e.resourceCrn) FROM Environment e " +
            "WHERE e.accountId = :accountId AND e.archived = false")
    List<ResourceWithId> findAsAuthorizationResourcesInAccount(@Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.cloudbreak.common.event.PayloadContext(e.resourceCrn, e.cloudPlatform) " +
            "FROM Environment e " +
            "WHERE e.id = :id")
    Optional<PayloadContext> findStackAsPayloadContext(@Param("id") Long id);

    @Query("SELECT e.resourceCrn as remoteResourceId, e.id as localId, e.name as name " +
            "FROM Environment e WHERE e.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Environment e WHERE e.resourceCrn = :resourceCrn")
    Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Environment e WHERE e.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT e FROM Environment e WHERE e.resourceCrn = :resourceCrn")
    Optional<Environment> findOneByResourceCrnEvenIfDeleted(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Environment e " +
            "WHERE e.name = :name " +
            "AND e.accountId = :accountId")
    Optional<ResourceBasicView> findResourceBasicViewByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name " +
            "FROM Environment e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);
}