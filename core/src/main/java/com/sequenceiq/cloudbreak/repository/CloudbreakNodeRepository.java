package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = CloudbreakNode.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface CloudbreakNodeRepository extends DisabledBaseRepository<CloudbreakNode, String> {
}
