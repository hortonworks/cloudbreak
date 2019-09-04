package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ConstraintTemplate.class)
public interface ConstraintTemplateRepository extends WorkspaceResourceRepository<ConstraintTemplate, Long> {

}
