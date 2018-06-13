package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@EntityType(entityClass = KerberosConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface KerberosConfigRepository extends CrudRepository<KerberosConfig, Long> {
}
