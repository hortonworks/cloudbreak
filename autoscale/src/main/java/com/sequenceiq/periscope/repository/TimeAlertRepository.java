package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.TimeAlert;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

@EntityType(entityClass = TimeAlert.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TimeAlertRepository extends CrudRepository<TimeAlert, Long> {

    TimeAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<TimeAlert> findAllByClusterIdOrderById(@Param("clusterId") Long clusterId);
}
