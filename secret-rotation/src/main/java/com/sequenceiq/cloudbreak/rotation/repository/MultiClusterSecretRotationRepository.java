package com.sequenceiq.cloudbreak.rotation.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.entity.MultiClusterSecretRotation;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = MultiClusterSecretRotation.class)
@Transactional(TxType.REQUIRED)
public interface MultiClusterSecretRotationRepository extends CrudRepository<MultiClusterSecretRotation, Long> {

    Optional<MultiClusterSecretRotation> findByResourceCrnAndSecretTypeAndResourceType(String resourceCrn, SecretType secretType,
            MultiClusterRotationResourceType resourceType);

}
