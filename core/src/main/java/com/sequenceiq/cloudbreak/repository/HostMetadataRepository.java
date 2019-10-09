package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = HostMetadata.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface HostMetadataRepository extends DisabledBaseRepository<HostMetadata, Long> {

    @Query("SELECT h FROM HostMetadata h WHERE h.hostGroup.cluster.id= :clusterId")
    Set<HostMetadata> findHostsInCluster(@Param("clusterId") Long clusterId);

    @Query("SELECT h FROM HostMetadata h "
            + "WHERE h.hostGroup.id= :hostGroupId AND (h.hostMetadataState= 'CONTAINER_RUNNING' OR h.hostMetadataState= 'SERVICES_RUNNING')")
    Set<HostMetadata> findEmptyHostsInHostGroup(@Param("hostGroupId") Long hostGroupId);

    @Query("SELECT h FROM HostMetadata h "
            + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostName = :hostName")
    Optional<HostMetadata> findHostInClusterByName(@Param("clusterId") Long clusterId, @Param("hostName") String hostName);

    @Query("SELECT h FROM HostMetadata h "
            + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostName = :hostName")
    Set<HostMetadata> findHostsInClusterByName(@Param("clusterId") Long clusterId, @Param("hostName") String hostName);

    @Query("SELECT COUNT(h) FROM HostMetadata h "
            + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostGroup.name = :hostGroupName")
    Long countByClusterIdAndHostGroupName(@Param("clusterId") Long id, @Param("hostGroupName") String hostGroupName);

    @Query("SELECT s.id as stackId, COUNT(h) as instanceCount "
            + "FROM HostMetadata h JOIN h.hostGroup hg JOIN hg.cluster c JOIN c.stack s WHERE s.workspace.id= :id AND h.hostMetadataState = 'UNHEALTHY' "
            + "GROUP BY s.id")
    Set<StackInstanceCount> countUnhealthyByWorkspaceId(@Param("id") Long workspaceId);
}
