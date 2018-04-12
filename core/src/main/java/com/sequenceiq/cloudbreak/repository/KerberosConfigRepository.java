package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@EntityType(entityClass = KerberosConfig.class)
public interface KerberosConfigRepository extends CrudRepository<KerberosConfig, Long> {
}
