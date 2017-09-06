package com.sequenceiq.cloudbreak.structuredevent.db;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.repository.EntityType;

@EntityType(entityClass = StructuredEventEntity.class)
public interface StructuredEventRepository extends CrudRepository<StructuredEventEntity, Long> {
}
