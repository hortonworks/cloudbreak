package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(TxType.REQUIRED)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    Optional<SecurityConfig> findOneByStackId(Long stackId);

}
