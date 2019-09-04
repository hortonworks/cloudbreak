package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = KubernetesConfig.class)
@Transactional(TxType.REQUIRED)
public interface KubernetesConfigRepository extends WorkspaceResourceRepository<KubernetesConfig, Long> {
}
