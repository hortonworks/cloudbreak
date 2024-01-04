package com.sequenceiq.cloudbreak.repository;


import jakarta.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackAuthentication.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackAuthenticationRepository extends CrudRepository<StackAuthentication, Long> {
}
