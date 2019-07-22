package com.sequenceiq.environment.network.dao.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface BaseNetworkRepository<T extends BaseNetwork> extends BaseJpaRepository<T, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<BaseNetwork> findByEnvironmentId(Long envId);

}
