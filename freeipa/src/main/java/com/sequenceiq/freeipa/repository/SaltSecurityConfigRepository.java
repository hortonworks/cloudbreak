package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@Transactional(TxType.REQUIRED)
public interface SaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long> {
}
