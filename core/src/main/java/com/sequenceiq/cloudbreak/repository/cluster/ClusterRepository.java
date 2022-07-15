package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.CertExpirationState;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends WorkspaceResourceRepository<Cluster, Long>, JpaRepository<Cluster, Long> {

    @Override
    Cluster save(Cluster entity);

    Optional<Cluster> findOneByStackId(long stackId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.gateway LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Optional<Cluster> findOneWithLists(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c WHERE c.stack IS NOT NULL AND c.stack.terminated IS NULL AND c.status IN :statuses")
    List<Cluster> findByStatuses(@Param("statuses") Collection<Status> statuses);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.stack WHERE c.workspace = null")
    Set<Cluster> findAllWithNoWorkspace();

    Set<Cluster> findByBlueprint(Blueprint blueprint);

    Set<Cluster> findByCustomConfigurations(CustomConfigurations customConfigurations);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.customConfigurations WHERE c.id = :id")
    Optional<Cluster> findOneWithCustomConfigurations(@Param("id") Long id);

    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<Cluster> findByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT c.name FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<String> findNamesByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT COUNT(c) FROM Cluster c WHERE c.workspace.id = :workspaceId AND c.environmentCrn = :environmentCrn AND c.status != 'DELETE_COMPLETED'")
    Long countAliveOnesByWorkspaceAndEnvironment(@Param("workspaceId") Long workspaceId, @Param("environmentCrn") String environmentCrn);

    @Modifying
    @Query("UPDATE Cluster c SET c.certExpirationState = :state WHERE c.id = :id")
    void updateCertExpirationState(@Param("id") Long id, @Param("state") CertExpirationState state);

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
    @Query("UPDATE Cluster c SET c.databusCredential = :databusCredential WHERE c.id = :clusterId")
    void updateDatabusCredentialByClusterId(@Param("clusterId") Long clusterId, @Param("databusCredential") String databusCredentialJsonString);

    @Modifying
    @Query(value = "INSERT INTO cluster_rdsconfig(clusters_id, rdsconfigs_id) VALUES (:clusterId, :rdsConfigId)", nativeQuery = true)
    void addRdsConfigToCluster(@Param("clusterId") Long clusterId, @Param("rdsConfigId") Long rdsConfigId);

    @Modifying
    @Query("UPDATE Cluster c SET c.fqdn = :fqdn WHERE c.id = :clusterId")
    void updateFqdnByClusterId(@Param("clusterId") Long clusterId, @Param("fqdn") String fqdn);

    @Modifying
    @Query("UPDATE Cluster c SET c.clusterManagerIp = :clusterManagerIp WHERE c.id = :clusterId")
    void updateClusterManagerIp(@Param("clusterId") Long clusterId, @Param("clusterManagerIp") String clusterManagerIp);
}
