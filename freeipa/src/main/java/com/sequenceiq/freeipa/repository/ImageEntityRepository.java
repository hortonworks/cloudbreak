package com.sequenceiq.freeipa.repository;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Transactional(Transactional.TxType.REQUIRED)
public interface ImageEntityRepository extends CrudRepository<ImageEntity, Long>, VaultRotationAwareRepository {

    @Override
    default Class<ImageEntity> getEntityClass() {
        return ImageEntity.class;
    }
}
