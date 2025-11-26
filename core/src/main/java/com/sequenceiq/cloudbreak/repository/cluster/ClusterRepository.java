package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.BaseBlueprintClusterView;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.CertExpirationState;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends WorkspaceResourceRepository<Cluster, Long>, JpaRepository<Cluster, Long>, VaultRotationAwareRepository {

    @Override
    Cluster save(Cluster entity);

    Optional<Cluster> findOneByStackId(long stackId);

    @Query(value = "SELECT id FROM cluster WHERE stack_id = :stackId", nativeQuery = true)
    Optional<Long> findClusterIdByStackId(@Param("stackId") long stackId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.gateway LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Optional<Cluster> findOneWithLists(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c WHERE c.stack IS NOT NULL AND c.stack.terminated IS NULL AND c.status IN :statuses")
    List<Cluster> findByStatuses(@Param("statuses") Collection<Status> statuses);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.stack WHERE c.workspace IS null")
    Set<Cluster> findAllWithNoWorkspace();

    @Modifying
    @Query(value = "UPDATE cluster "
            + "SET blueprint_id = null "
            + "WHERE blueprint_id = :blueprintId "
            + "AND status= 'DELETE_COMPLETED'", nativeQuery = true)
    void deleteBlueprintRelationWhereClusterDeleted(@Param("blueprintId") Long blueprintId);

    @Query("SELECT s.name as name, s.type as type, s.id as id FROM Cluster c " +
            "JOIN c.stack s " +
            "WHERE c.blueprint.id = :blueprintId")
    Set<BaseBlueprintClusterView> findByStackResourceCrn(@Param("blueprintId") Long blueprintId);

    Set<Cluster> findByCustomConfigurations(CustomConfigurations customConfigurations);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.customConfigurations WHERE c.id = :id")
    Optional<Cluster> findOneWithCustomConfigurations(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<Cluster> findByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT c.name FROM Cluster c JOIN c.hostGroups hg WHERE hg.id IN :hostGroupIds")
    Set<String> findAllClusterNamesByHostGroupIds(@Param("hostGroupIds") Set<Long> hostGroupIds);

    @Query("SELECT count(c) FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    int countByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT c.name FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<String> findNamesByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT COUNT(c) FROM Cluster c WHERE c.workspace.id = :workspaceId AND c.environmentCrn = :environmentCrn AND c.status != 'DELETE_COMPLETED'")
    Long countAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Modifying
    @Query("UPDATE Cluster c SET c.certExpirationState = :state, c.certExpirationDetails = :details WHERE c.id = :id")
    void updateCertExpirationState(@Param("id") Long id, @Param("state") CertExpirationState state, @Param("details") String certExpirationDetails);

    @Query("SELECT c FROM Cluster c " +
            "JOIN c.stack s " +
            "WHERE s.resourceCrn = :stackResourceCrn")
    Optional<Cluster> findByStackResourceCrn(@Param("stackResourceCrn") String stackResourceCrn);

    @Modifying
    @Query("UPDATE Cluster c SET c.creationStarted = :creationStarted WHERE c.id = :clusterId")
    void updateCreationStartedByClusterId(@Param("clusterId") Long clusterId, @Param("creationStarted") long creationStarted);

    @Modifying
    @Query("UPDATE Cluster c SET c.upSince = :now WHERE c.id = :clusterId")
    void updateUpSinceByClusterId(@Param("clusterId") Long clusterId, @Param("now") long now);

    @Modifying
    @Query("UPDATE Cluster c SET c.creationFinished = :now, c.upSince = :now WHERE c.id = :clusterId")
    void updateCreationFinishedAndUpSinceByClusterId(@Param("clusterId") Long clusterId, @Param("now") long now);

    @Modifying
    @Query(value = "INSERT INTO cluster_rdsconfig(clusters_id, rdsconfigs_id) VALUES (:clusterId, :rdsConfigId)", nativeQuery = true)
    void addRdsConfigToCluster(@Param("clusterId") Long clusterId, @Param("rdsConfigId") Long rdsConfigId);

    @Modifying
    @Query("UPDATE Cluster c SET c.fqdn = :fqdn WHERE c.id = :clusterId")
    void updateFqdnByClusterId(@Param("clusterId") Long clusterId, @Param("fqdn") String fqdn);

    @Modifying
    @Query("UPDATE Cluster c SET c.clusterManagerIp = :clusterManagerIp WHERE c.id = :clusterId")
    void updateClusterManagerIp(@Param("clusterId") Long clusterId, @Param("clusterManagerIp") String clusterManagerIp);

    @Modifying
    @Query("UPDATE Cluster c SET c.dbSslEnabled = true WHERE c.id = :clusterId")
    void enableSsl(@Param("clusterId") Long clusterId);

    @Modifying
    @Query("UPDATE Cluster c SET c.dbSslRootCertBundle = :dbSslRootCertBundle, c.dbSslEnabled = :dbSslEnabled WHERE c.id = :clusterId")
    void updateDbSslCert(@Param("clusterId") Long clusterId,
            @Param("dbSslRootCertBundle") String dbSslRootCertBundle,
            @Param("dbSslEnabled") Boolean dbSslEnabled);

    @Query("SELECT c.name as name " +
            "FROM Cluster c " +
            "JOIN Stack s ON s.id =  c.stack.id " +
            "WHERE c.encryptionProfileCrn = :crn " +
            "AND s.terminated IS NUll ")
    List<String> findAllClusterNamesByEncryptionProfileCrn(@Param("crn") String crn);

    @Override
    default Class<Cluster> getEntityClass() {
        return Cluster.class;
    }
}
