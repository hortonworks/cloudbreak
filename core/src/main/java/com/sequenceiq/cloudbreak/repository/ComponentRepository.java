package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;

@EntityType(entityClass = Component.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ComponentRepository extends CrudRepository<Component, Long> {

    @Query("SELECT cv FROM Component cv WHERE cv.stack.id = :stackId AND cv.componentType = :componentType AND cv.name = :name")
    Component findComponentByStackIdComponentTypeName(@Param("stackId") Long stackId, @Param("componentType") ComponentType componentType,
        @Param("name") String name);

    @Query("SELECT cv FROM Component cv WHERE cv.stack.id = :stackId")
    Set<Component> findComponentByStackId(@Param("stackId") Long stackId);

    @Query("SELECT cv FROM Component cv WHERE cv.stack.id = :stackId AND cv.componentType in :componentTypes")
    Set<Component> findComponentByStackIdWithType(@Param("stackId") Long stackId, @Param("componentTypes") Set<ComponentType> componentTypes);
}