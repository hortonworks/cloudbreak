package com.sequenceiq.environment.environment.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.environment.domain.LbUpdateFlowLog;

@Transactional(Transactional.TxType.REQUIRED)
public interface LbUpdateFlowLogRepository extends CrudRepository<LbUpdateFlowLog, Long> {

    Set<LbUpdateFlowLog> findByParentFlowId(@Param("parentFlowId") String parentFlowId);

    Set<LbUpdateFlowLog> findByChildFlowId(@Param("childFlowId") String childFlowId);
}
