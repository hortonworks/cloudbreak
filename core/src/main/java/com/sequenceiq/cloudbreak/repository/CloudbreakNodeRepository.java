package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = CloudbreakNode.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface CloudbreakNodeRepository extends DisabledBaseRepository<CloudbreakNode, String> {
}
