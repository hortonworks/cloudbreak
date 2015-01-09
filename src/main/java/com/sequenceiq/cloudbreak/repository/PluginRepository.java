package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Plugin;

public interface PluginRepository extends CrudRepository<Plugin, Long> {

    Set<Plugin> findAllForRecipe(@Param("recipeId") Long recipeId);

}
