package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = CloudbreakNode.class)
public interface CloudbreakNodeRepository extends CrudRepository<CloudbreakNode, String> {
}
