package com.sequenceiq.periscope.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.History;

@EntityType(entityClass = History.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface HistoryRepository extends CrudRepository<History, Long> {

    List<History> findByClusterId(@Param("clusterId")Long clusterId, Pageable pageRequest);
}
