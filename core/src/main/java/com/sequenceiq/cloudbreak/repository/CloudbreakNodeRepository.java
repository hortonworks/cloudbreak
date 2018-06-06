package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;

@EntityType(entityClass = CloudbreakNode.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CloudbreakNodeRepository extends CrudRepository<CloudbreakNode, String> {
}
