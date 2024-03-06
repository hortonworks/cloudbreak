package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = LoadBalancer.class)
@Transactional(TxType.REQUIRED)
public interface LoadBalancerRepository extends CrudRepository<LoadBalancer, Long> {

    Set<LoadBalancer> findByStackId(@Param("stackId") Long stackId);

    void deleteByStackId(@Param("stackId") Long stackId);

}
