package com.sequenceiq.cloudbreak.ha.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Node.class)
@Transactional(TxType.REQUIRED)
public interface NodeRepository extends JpaRepository<Node, String> {
}
