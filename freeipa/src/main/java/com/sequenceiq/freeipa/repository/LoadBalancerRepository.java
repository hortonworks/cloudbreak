package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.LoadBalancer;

@Transactional(TxType.REQUIRED)
public interface LoadBalancerRepository extends CrudRepository<LoadBalancer, Long> {

    Optional<LoadBalancer> findByStackId(@Param("stackId") Long stackId);
}
