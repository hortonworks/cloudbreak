package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.periscope.domain.History;

public interface HistoryRepository extends CrudRepository<History, Long> {

    List<History> findAllByCluster(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    History findByCluster(@Param("clusterId") Long clusterId, @Param("historyId") Long historyId);

}
