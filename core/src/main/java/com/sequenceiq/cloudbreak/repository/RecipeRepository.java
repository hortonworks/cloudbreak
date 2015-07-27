package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Recipe;

@EntityType(entityClass = Recipe.class)
public interface RecipeRepository extends CrudRepository<Recipe, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Recipe findOne(@Param("id") Long id);

    Recipe findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<Recipe> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    Set<Recipe> findAllInAccount(@Param("account") String account);

    Set<Recipe> findForUser(@Param("owner") String userId);

    Recipe findByNameForUser(@Param("name") String name, @Param("owner") String userId);

}
