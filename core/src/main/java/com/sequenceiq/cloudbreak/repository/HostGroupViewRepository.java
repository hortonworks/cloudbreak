package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = HostGroupView.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface HostGroupViewRepository extends DisabledBaseRepository<HostGroupView, Long> {

    @Query("SELECT h FROM HostGroupView h LEFT JOIN FETCH h.hostMetadata hm LEFT JOIN FETCH h.cluster c WHERE c.id IN :clusterIds")
    Set<HostGroupView> findHostGroupsInClusterList(@Param("clusterIds") Set<Long> clusterIds);
}
