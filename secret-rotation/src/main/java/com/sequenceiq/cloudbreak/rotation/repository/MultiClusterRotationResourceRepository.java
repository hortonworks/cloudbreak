package com.sequenceiq.cloudbreak.rotation.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = MultiClusterRotationResource.class)
@Transactional(TxType.REQUIRED)
public interface MultiClusterRotationResourceRepository extends CrudRepository<MultiClusterRotationResource, Long> {

    Optional<MultiClusterRotationResource> findByResourceCrnAndSecretTypeAndType(String resourceCrn, MultiSecretType secretType,
            MultiClusterRotationResourceType type);

    Set<MultiClusterRotationResource> findAllBySecretTypeAndResourceCrn(MultiSecretType secretType, String resourceCrn);

    Set<MultiClusterRotationResource> findAllBySecretTypeAndResourceCrnIn(MultiSecretType secretType, Set<String> crns);

    void deleteAllByResourceCrn(String crn);
}
