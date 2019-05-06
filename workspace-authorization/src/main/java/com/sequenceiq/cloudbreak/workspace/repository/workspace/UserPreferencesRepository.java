package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.UserPreferences;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = UserPreferences.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface UserPreferencesRepository extends DisabledBaseRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUser(User user);

}
