package com.sequenceiq.freeipa.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;

@Transactional(Transactional.TxType.REQUIRED)
public interface FreeIpaStackRecipeRepository extends CrudRepository<FreeIpaStackRecipe, Long> {

    List<FreeIpaStackRecipe> findByStackId(Long stackId);

    void deleteFreeIpaStackRecipesByStackId(Long stackId);

}
