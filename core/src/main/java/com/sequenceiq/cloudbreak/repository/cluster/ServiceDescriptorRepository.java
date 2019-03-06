package com.sequenceiq.cloudbreak.repository.cluster;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = ServiceDescriptor.class)
@Transactional(TxType.REQUIRED)
public interface ServiceDescriptorRepository  extends DisabledBaseRepository<ServiceDescriptor, Long> {
}
