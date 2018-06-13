package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    SecurityConfig findOneByStackId(Long stackId);

}
