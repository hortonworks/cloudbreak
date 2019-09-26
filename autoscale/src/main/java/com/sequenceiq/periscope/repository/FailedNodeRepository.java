package com.sequenceiq.periscope.repository;

import java.util.List;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.repository.BaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.FailedNode;

@DisableHasPermission
@EntityType(entityClass = FailedNode.class)
public interface FailedNodeRepository extends BaseRepository<FailedNode, Long> {

    List<FailedNode> findByClusterId(long clusterId);

}