package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.repository.BaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.History;

@HasPermission
@EntityType(entityClass = History.class)
public interface HistoryRepository extends BaseRepository<History, Long> {

    List<History> findAllByCluster(@Param("id") Long id);

    History findByCluster(@Param("clusterId") Long clusterId, @Param("historyId") Long historyId);

}
