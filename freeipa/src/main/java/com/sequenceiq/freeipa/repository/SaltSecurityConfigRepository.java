package com.sequenceiq.freeipa.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@Transactional(TxType.REQUIRED)
public interface SaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long>, VaultRotationAwareRepository {

    @Override
    default Class<SaltSecurityConfig> getEntityClass() {
        return SaltSecurityConfig.class;
    }
}
