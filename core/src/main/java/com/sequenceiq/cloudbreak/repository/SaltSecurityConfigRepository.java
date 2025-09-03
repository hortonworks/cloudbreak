package com.sequenceiq.cloudbreak.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = SaltSecurityConfig.class)
@Transactional(TxType.REQUIRED)
public interface SaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long>, VaultRotationAwareRepository {

    @Override
    default Class<SaltSecurityConfig> getEntityClass() {
        return SaltSecurityConfig.class;
    }

}
