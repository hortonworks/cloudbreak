package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Recipe;

public interface RecipeRepository extends CrudRepository<Recipe, Long> {

    Recipe findByNameInAccount(@Param("name") String name, @Param("account") String account);
}
