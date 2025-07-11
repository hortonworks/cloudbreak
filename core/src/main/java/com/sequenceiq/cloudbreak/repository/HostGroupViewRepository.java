package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = HostGroupView.class)
@Transactional(TxType.REQUIRED)
public interface HostGroupViewRepository extends CrudRepository<HostGroupView, Long> {

    @Query("SELECT h FROM HostGroupView h WHERE h.clusterId IN :clusterIds")
    Set<HostGroupView> findHostGroupsInClusterList(@Param("clusterIds") Set<Long> clusterIds);
}
