package com.sequenceiq.environment.parameters.dao.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface BaseParametersRepository<T extends BaseParameters> extends BaseJpaRepository<T, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<BaseParameters> findByEnvironmentId(Long envId);

}
