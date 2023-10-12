package com.sequenceiq.periscope.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.PeriscopeJob;

@EntityType(entityClass = PeriscopeJob.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface PeriscopeJobRepository extends CrudRepository<PeriscopeJob, String> {

}

