package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Component;

@EntityType(entityClass = Component.class)
public interface ComponentRepository extends CrudRepository<Component, Long> {

    Component findComponentByStackIdComponentTypeName(@Param("stackId") Long stackId, @Param("componentType") ComponentType componentType,
            @Param("name") String name);

    Set<Component> findComponentByStackId(@Param("stackId") Long stackId);
}