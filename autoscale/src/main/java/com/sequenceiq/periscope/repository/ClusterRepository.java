package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;

public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    Cluster findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    Cluster findByStackId(@Param("stackId") Long stackId);

    Cluster findById(Long id);

    List<Cluster> findByUserId(String id);

    List<Cluster> findByState(ClusterState state);

    List<Cluster> findByStateAndAutoscalingEnabled(ClusterState state, boolean autoscalingEnabled);
}
