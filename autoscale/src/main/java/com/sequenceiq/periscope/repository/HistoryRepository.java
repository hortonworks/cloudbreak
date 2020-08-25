package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.History;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

@EntityType(entityClass = History.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface HistoryRepository extends CrudRepository<History, Long> {

    List<History> findByClusterId(@Param("clusterId")Long clusterId, Pageable pageRequest);
}
