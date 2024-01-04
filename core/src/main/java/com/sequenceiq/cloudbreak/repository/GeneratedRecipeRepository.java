package com.sequenceiq.cloudbreak.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = GeneratedRecipe.class)
@Transactional(TxType.REQUIRED)
public interface GeneratedRecipeRepository extends CrudRepository<GeneratedRecipe, Long> {

}
