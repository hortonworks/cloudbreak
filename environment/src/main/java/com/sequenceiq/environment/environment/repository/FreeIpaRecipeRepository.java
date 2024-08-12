package com.sequenceiq.environment.environment.repository;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.environment.domain.FreeIpaRecipe;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = FreeIpaRecipe.class)
public interface FreeIpaRecipeRepository extends JpaRepository<FreeIpaRecipe, Long> {

    List<FreeIpaRecipe> findByEnvironmentId(Long environmentId);

    @Modifying
    @Query("DELETE FROM FreeIpaRecipe fr WHERE fr.environmentId = :environmentId")
    void deleteFreeIpaRecipesByEnvironmentId(Long environmentId);

}
