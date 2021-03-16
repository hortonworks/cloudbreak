package com.sequenceiq.datalake.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.projection.SdxClusterIdView;

@Repository
@Transactional(TxType.REQUIRED)
public interface SdxClusterRepository extends CrudRepository<SdxCluster, Long> {

    @Override
    List<SdxCluster> findAll();

    @Query("SELECT s.id as id, s.stackCrn as stackCrn " +
            "FROM SdxCluster s " +
            "WHERE s.deleted is null " +
            "AND s.stackCrn is not null")
    List<SdxClusterIdView> findAllAliveView();

    Optional<SdxCluster> findByAccountIdAndClusterNameAndDeletedIsNull(String accountId, String clusterName);

    Optional<SdxCluster> findByAccountIdAndCrnAndDeletedIsNull(String accountId, String crn);

    @Query("SELECT s.envCrn FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn = :crn AND s.deleted is null")
    Optional<String> findEnvCrnByAccountIdAndCrnAndDeletedIsNull(@Param("accountId") String accountId, @Param("crn") String crn);

    @Query("SELECT s FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn IN (:crns) AND s.deleted is null")
    List<SdxCluster> findAllByAccountIdAndCrnAndDeletedIsNull(@Param("accountId") String accountId, @Param("crns") Set<String> crns);

    @Query("SELECT s.clusterName as name, s.crn as crn FROM SdxCluster s WHERE s.accountId = :accountId AND s.crn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    List<SdxCluster> findByAccountIdAndDeletedIsNull(String accountId);

    List<SdxCluster> findByAccountIdAndEnvCrnAndDeletedIsNull(String accountId, String envCrn);

    List<SdxCluster> findByAccountIdAndEnvNameAndDeletedIsNull(String accountId, String envName);

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
            "WHERE s.accountId = :accountId AND s.envCrn = :envCrn AND s.deleted IS NULL")
    List<ResourceWithId> findAuthorizationResourcesByAccountIdAndEnvCrn(@Param("accountId") String accountId, @Param("envCrn") String envCrn);
}
