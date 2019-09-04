package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SaltSecurityConfig.class)
@Transactional(TxType.REQUIRED)
public interface SaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long> {

}
