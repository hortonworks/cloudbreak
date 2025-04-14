package com.sequenceiq.freeipa.repository;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.DynamicEntitlement;

@Transactional(Transactional.TxType.REQUIRED)
public interface DynamicEntitlementRepository extends CrudRepository<DynamicEntitlement, Long> {

    Set<DynamicEntitlement> findByStackId(@Param("stackId") Long stackId);

}
