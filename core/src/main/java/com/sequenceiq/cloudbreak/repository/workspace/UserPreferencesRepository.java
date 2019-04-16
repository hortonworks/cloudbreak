package com.sequenceiq.cloudbreak.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserPreferences.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface UserPreferencesRepository extends DisabledBaseRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUser(User user);

}
