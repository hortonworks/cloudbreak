package com.sequenceiq.environment.environment.repository;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.environment.domain.FreeIpaRecipe;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = FreeIpaRecipe.class)
public interface FreeIpaRecipeRepository extends JpaRepository<FreeIpaRecipe, Long> {

    List<FreeIpaRecipe> findByEnvironmentId(Long environmentId);

    void deleteFreeIpaRecipesByEnvironmentId(Long environmentId);

}
