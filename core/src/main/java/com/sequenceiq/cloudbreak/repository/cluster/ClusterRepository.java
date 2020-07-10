package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.UptimeStat;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Cluster.class)
public interface ClusterRepository extends WorkspaceResourceRepository<Cluster, Long> {

    @Override
    Cluster save(Cluster entity);

    Optional<Cluster> findOneByStackId(long stackId);

    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.gateway LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Optional<Cluster> findOneWithLists(@Param("id") Long id);

    Set<Cluster> findByBlueprint(Blueprint blueprint);

    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<Cluster> findByRdsConfig(@Param("id") Long rdsConfigId);

    @Query("SELECT c.name FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id AND c.status != 'DELETE_COMPLETED'")
    Set<String> findNamesByRdsConfig(@Param("id") Long rdsConfigId);

    @Modifying
    @Query("UPDATE Cluster c SET c.upSince = :upSince WHERE c.stack.id = :stackId")
    int updateUpSinceByStackId(@Param("stackId") long stackId, @Param("upSince") long upSince);

    @Modifying
    @Query("UPDATE Cluster c SET c.uptime = :uptime WHERE c.stack.id = :stackId")
    int updateUptimeByStackId(@Param("stackId") long stackId, @Param("uptime") String uptime);

    @Query("SELECT new com.sequenceiq.cloudbreak.domain.stack.cluster.UptimeStat(c.upSince, c.uptime) FROM Cluster c WHERE c.stack.id = :stackId")
    UptimeStat findUptimeStatByStackId(@Param("stackId") long stackId);
}
