package com.sequenceiq.freeipa.repository;

import java.util.Collection;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;

@Transactional(Transactional.TxType.REQUIRED)
public interface FreeIpaStackRecipeRepository extends CrudRepository<FreeIpaStackRecipe, Long> {

    List<FreeIpaStackRecipe> findByStackIdIn(List<Long> stackIds);

    List<FreeIpaStackRecipe> findByStackId(Long stackId);

    void deleteFreeIpaStackRecipesByStackId(Long stackId);

    void deleteFreeIpaStackRecipeByStackIdAndRecipeIn(Long stackId, Collection<String> names);

}
