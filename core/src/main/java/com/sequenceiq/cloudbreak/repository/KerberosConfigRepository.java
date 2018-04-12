package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = KerberosConfig.class)
public interface KerberosConfigRepository extends CrudRepository<KerberosConfig, Long> {
}
