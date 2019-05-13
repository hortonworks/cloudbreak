package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowLog;

@NoRepositoryBean
@Transactional(Transactional.TxType.REQUIRED)
public interface ServiceFlowLogRepository extends Repository<FlowLog, Long> {
    int purgeTerminatedStackLogs();

    Set<Long> findTerminatingStacksByCloudbreakNodeId(@Param("cloudbreakNodeId") String cloudbreakNodeId);
}
