package com.sequenceiq.cloudbreak.repository.cluster;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ServiceDescriptor.class)
@Transactional(TxType.REQUIRED)
public interface ServiceDescriptorRepository  extends CrudRepository<ServiceDescriptor, Long> {
}
