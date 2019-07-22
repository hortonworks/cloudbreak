package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface StackAuthenticationRepository extends BaseCrudRepository<StackAuthentication, Long> {
}
