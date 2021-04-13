package com.sequenceiq.cloudbreak.cm;


import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = LoadBalancer.class)
@Transactional(TxType.REQUIRED)
public interface LoadBalancerRepositoryDos extends CrudRepository<LoadBalancer, Long> {

    Set<LoadBalancer> findByStackId(@Param("stackId") Long stackId);
}
