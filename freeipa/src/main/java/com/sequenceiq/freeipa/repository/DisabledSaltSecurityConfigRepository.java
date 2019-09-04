package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@Transactional(TxType.REQUIRED)
public interface DisabledSaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long> {

    @Override
    SaltSecurityConfig save(SaltSecurityConfig entity);
}
