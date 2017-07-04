package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;

@EntityType(entityClass = CloudbreakNode.class)
public interface CloudbreakNodeRepository extends CrudRepository<CloudbreakNode, String> {
}
