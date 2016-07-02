package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;

@EntityType(entityClass = Resource.class)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    Resource findOne(@Param("id") Long id);

    Resource findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    Resource findByStackIdAndResourceNameOrReference(@Param("stackId") Long stackId, @Param("resource") String resource);
}
