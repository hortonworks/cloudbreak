package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.EntityType;

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

}
