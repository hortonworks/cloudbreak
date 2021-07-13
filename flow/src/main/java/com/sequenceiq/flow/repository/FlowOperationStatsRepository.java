package com.sequenceiq.flow.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.domain.FlowOperationStats;

@Transactional(Transactional.TxType.REQUIRED)
public interface FlowOperationStatsRepository extends CrudRepository<FlowOperationStats, Long> {

    Optional<FlowOperationStats> findFirstByOperationTypeAndCloudPlatform(@Param("operationType") OperationType operationType,
            @Param("cloudPlatform") String cloudPlatform);

}
