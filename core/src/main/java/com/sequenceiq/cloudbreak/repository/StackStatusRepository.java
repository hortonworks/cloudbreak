package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.StackStatus;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = StackStatus.class)
public interface StackStatusRepository extends CrudRepository<StackStatus, Long> {

    StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId);
}
