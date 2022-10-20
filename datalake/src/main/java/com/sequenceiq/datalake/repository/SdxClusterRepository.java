package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.datalake.entity.SdxCluster;

@Repository
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = SdxCluster.class)
public interface SdxClusterRepository extends AccountAwareResourceRepository<SdxCluster, Long>, JobResourceRepository<SdxCluster, Long> {

    @Override
    List<SdxCluster> findAll();

    @Query("SELECT s.id as localId, s.stackCrn as remoteResourceId, s.name as name " +
            "FROM SdxCluster s " +
            "WHERE s.deleted is null " +
            "AND s.stackCrn is not null")
    List<JobResource> findAllAliveView();

    Optional<SdxCluster> findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(String accountId, String clusterName);

    Optional<SdxCluster> findByAccountIdAndClusterNameAndDeletedIsNull(String accountId, String clusterName);

    Optional<SdxCluster> findByAccountIdAndCrnAndDeletedIsNull(String accountId, String crn);

    List<SdxCluster> findByAccountIdAndOriginalCrnAndDeletedIsNull(String accountId, String crn);

    List<SdxCluster> findByAccountIdAndOriginalCrn(String accountId, String crn);

    Optional<SdxCluster> findByCrnAndDeletedIsNull(String crn);

    Optional<SdxCluster> findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(@Param("accountId") String accountId, @Param("crn") String crn);

    List<SdxCluster> findByAccountIdAndEnvCrn(@Param("accountId") String accountId, @Param("envcrn") String envcrn);

    @Query("SELECT s.envCrn FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn = :crn AND s.deleted is null")
    Optional<String> findEnvCrnByAccountIdAndCrnAndDeletedIsNull(@Param("accountId") String accountId, @Param("crn") String crn);

    @Query("SELECT s FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn IN (:crns) AND s.deleted is null AND s.detached = false")
    List<SdxCluster> findAllByAccountIdAndCrnAndDeletedIsNullAndDetachedIsFalse(@Param("accountId") String accountId, @Param("crns") Set<String> crns);

    @Query("SELECT s.clusterName as name, s.crn as crn FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    List<SdxCluster> findByAccountIdAndDeletedIsNull(String accountId);

    List<SdxCluster> findByAccountIdAndDeletedIsNullAndDetachedIsFalse(String accountId);

    List<SdxCluster> findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(String accountId, String envCrn);

    List<SdxCluster> findByAccountIdAndEnvCrnAndDeletedIsNull(String accountId, String envCrn);

    List<SdxCluster> findByAccountIdAndEnvNameAndDeletedIsNull(String accountId, String envName);

    List<SdxCluster> findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(String accountId, String envName);

    @Query("SELECT s.stackCrn FROM SdxCluster s WHERE s.crn = :crn")
    Optional<String> findStackCrnByClusterCrn(@Param("crn") String crn);

    @Modifying
    @Query("UPDATE SdxCluster s SET s.certExpirationState = :state WHERE s.id = :id")
    void updateCertExpirationState(@Param("id") Long id, @Param("state") CertExpirationState state);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountId(@Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.crn IN :crns AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndCrns(@Param("accountId") String accountId, @Param("crns") List<String> crns);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.envName = :envName AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndEnvName(@Param("accountId") String accountId, @Param("envName") String envName);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.envName IN (:envNames) AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndEnvNames(@Param("accountId") String accountId, @Param("envNames") List<String> envNames);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.envCrn = :envCrn AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndEnvCrn(@Param("accountId") String accountId, @Param("envCrn") String envCrn);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(s.id, s.crn, s.envCrn) FROM SdxCluster s " +
            "WHERE s.accountId = :accountId AND s.envCrn IN (:envCrns) AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndEnvCrns(@Param("accountId") String accountId, @Param("envCrns") List<String> envCrns);

    @Query("SELECT s.id as localId, s.stackCrn as remoteResourceId, s.name as name " +
            "FROM SdxCluster s " +
            "WHERE s.id = :resourceId")
    Optional<JobResource> getJobResource(@Param("resourceId") Long resourceId);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name " +
            "FROM SdxCluster s WHERE s.resourceCrn = :resourceCrn")
    Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name " +
            "FROM SdxCluster s WHERE s.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name " +
            "FROM SdxCluster s " +
            "WHERE s.name = :name " +
            "AND s.accountId = :accountId")
    Optional<ResourceBasicView> findResourceBasicViewByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT s.resourceCrn as resourceCrn, s.id as id, s.name as name " +
            "FROM SdxCluster s " +
            "WHERE s.name in (:names) " +
            "AND s.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Modifying
    @Query("UPDATE SdxCluster s SET s.databaseEngineVersion = :databaseEngineVersion WHERE s.crn = :crn")
    int updateDatabaseEngineVersion(@Param("crn") String crn, @Param("databaseEngineVersion") String externalDatabaseEngineVersion);

    @Query("SELECT sdxc.resourceCrn FROM SdxCluster sdxc WHERE sdxc.id = :id")
    Optional<String> findCrnById(@Param("id") Long id);

}
