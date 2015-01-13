package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.KeyValue;

public interface KeyValueRepository extends CrudRepository<KeyValue, Long> {

    Set<KeyValue> findAllForRecipe(@Param("recipeId") Long recipeId);
}
