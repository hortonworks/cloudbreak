package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.History;

@EntityType(entityClass = History.class)
public interface HistoryRepository extends CrudRepository<History, Long> {

    List<History> findAllByCluster(@Param("id") Long id);

    History findByCluster(@Param("clusterId") Long clusterId, @Param("historyId") Long historyId);

}
