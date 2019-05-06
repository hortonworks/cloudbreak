package com.sequenceiq.cloudbreak.repository.cluster;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;

@DisableHasPermission
@EntityType(entityClass = ServiceDescriptor.class)
@Transactional(TxType.REQUIRED)
public interface ServiceDescriptorRepository  extends DisabledBaseRepository<ServiceDescriptor, Long> {
}
