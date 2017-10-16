package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;

@EntityType(entityClass = SecurityConfig.class)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    SecurityConfig findOneByStackId(Long stackId);

}
