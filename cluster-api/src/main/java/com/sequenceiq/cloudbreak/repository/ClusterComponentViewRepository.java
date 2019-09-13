package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ClusterComponentView.class)
@Transactional(TxType.REQUIRED)
public interface ClusterComponentViewRepository extends CrudRepository<ClusterComponentView, Long> {
    ClusterComponentView findOneByClusterIdAndComponentTypeAndName(@Param("clusterId") Long clusterId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);
}
