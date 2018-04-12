package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = SecurityConfig.class)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    SecurityConfig findOneByStackId(Long stackId);

}
