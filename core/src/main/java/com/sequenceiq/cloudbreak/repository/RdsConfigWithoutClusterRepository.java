package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = RDSConfig.class)
@Transactional(TxType.REQUIRED)
public interface RdsConfigWithoutClusterRepository extends CrudRepository<RDSConfig, Long> {

    String PROJECTION = "r.id as id, " +
            "r.name as name, " +
            "r.description as description, " +
            "r.connectionURL as connectionURL," +
            "r.sslMode as sslMode," +
            "r.databaseEngine as databaseEngine," +
            "r.connectionDriver as connectionDriver," +
            "r.creationDate as creationDate," +
            "r.stackVersion as stackVersion," +
            "r.status as status," +
            "r.type as type," +
            "r.connectorJarUrl as connectorJarUrl," +
            "r.archived as archived," +
            "r.deletionTimestamp as deletionTimestamp," +
            "r.connectionUserName as connectionUserName," +
            "r.connectionPassword as connectionPassword ";

    @Query("SELECT " + PROJECTION +
            "FROM RDSConfig r " +
            "INNER JOIN r.clusters c " +
            "WHERE c.id= :clusterId " +
            "AND r.status <> 'DEFAULT_DELETED' AND r.type= :type")
    RdsConfigWithoutCluster findByClusterIdAndType(@Param("clusterId") Long clusterId, @Param("type") String type);

    @Query("SELECT " + PROJECTION +
            "FROM RDSConfig r " +
            "INNER JOIN r.clusters c " +
            "WHERE c.id = :clusterId")
    Set<RdsConfigWithoutCluster> findByClusterId(@Param("clusterId") Long clusterId);

    @Query("SELECT DISTINCT COUNT(r) FROM RDSConfig r " +
            "INNER JOIN r.clusters c " +
            "WHERE c.id= :clusterId " +
            "AND r.type in (:databaseTypes) " +
            "AND r.status in (:statuses)")
    long countByClusterIdAndStatusAndTypeIn(@Param("clusterId") Long id,
            @Param("statuses") Set<ResourceStatus> statuses,
            @Param("databaseTypes") Set<String> databaseTypes);

    @Query("SELECT DISTINCT " + PROJECTION + " FROM RDSConfig r " +
            "INNER JOIN r.clusters c " +
            "WHERE c.id= :clusterId " +
            "AND r.type in (:databaseTypes) " +
            "AND r.status in (:statuses)")
    List<RdsConfigWithoutCluster> findByClusterIdAndStatusInAndTypeIn(@Param("clusterId") Long id,
            @Param("statuses") Set<ResourceStatus> statuses,
            @Param("databaseTypes") Set<String> databaseTypes);
}
