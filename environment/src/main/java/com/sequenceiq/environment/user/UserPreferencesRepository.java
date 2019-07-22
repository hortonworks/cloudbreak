package com.sequenceiq.environment.user;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface UserPreferencesRepository extends BaseCrudRepository<UserPreferences, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<UserPreferences> findByUserCrn(String userCrn);

}
