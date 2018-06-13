package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@EntityType(entityClass = Recipe.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface RecipeRepository extends CrudRepository<Recipe, Long> {

    @Query("SELECT r FROM Recipe r WHERE r.name= :name AND r.account= :account")
    Recipe findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT r FROM Recipe r WHERE r.name IN :names AND r.account= :account")
    Set<Recipe> findByNameInAccount(@Param("names") Collection<String> names, @Param("account") String account);

    @Query("SELECT r FROM Recipe r WHERE (r.account= :account AND r.publicInAccount= true AND r.recipeType "
            + "NOT IN ('LEGACY','MIGRATED')) OR (r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED'))")
    Set<Recipe> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    @Query("SELECT r FROM Recipe r WHERE r.account= :account AND r.recipeType NOT IN ('LEGACY','MIGRATED')")
    Set<Recipe> findAllInAccount(@Param("account") String account);

    @Query("SELECT r FROM Recipe r WHERE r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED')")
    Set<Recipe> findForUser(@Param("owner") String userId);

    @Query("SELECT r FROM Recipe r WHERE recipeType= :recipeType")
    Set<Recipe> findByType(@Param("recipeType") RecipeType recipeType);

    @Query("SELECT r FROM Recipe r WHERE r.name= :name AND r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED')")
    Recipe findByNameForUser(@Param("name") String name, @Param("owner") String userId);

}
