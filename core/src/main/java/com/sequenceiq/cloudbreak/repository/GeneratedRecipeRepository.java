package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = GeneratedRecipe.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface GeneratedRecipeRepository extends DisabledBaseRepository<GeneratedRecipe, Long> {

}
