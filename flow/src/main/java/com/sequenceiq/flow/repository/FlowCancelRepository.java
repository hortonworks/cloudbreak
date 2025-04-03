package com.sequenceiq.flow.repository;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.flow.domain.FlowCancel;

@EntityType(entityClass = FlowCancel.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface FlowCancelRepository extends JpaRepository<FlowCancel, Long> {
}
