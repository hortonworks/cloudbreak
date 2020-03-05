package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface UserSyncStatusRepository extends BaseCrudRepository<UserSyncStatus, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<UserSyncStatus> getByStack(Stack stack);

    @Override
    @CheckPermission(action = ResourceAction.READ)
    <S extends UserSyncStatus> S save(S entity);
}
