package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Recipe;

public interface RecipeRepository extends CrudRepository<Recipe, Long> {

    Recipe findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<Recipe> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    Set<Recipe> findAllInAccount(@Param("account") String account);

    Set<Recipe> findForUser(@Param("owner") String userId);

    Recipe findByNameForUser(@Param("name") String name, @Param("owner") String userId);

}
