package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.UserPreferences;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = UserPreferences.class)
@Transactional(TxType.REQUIRED)
public interface UserPreferencesRepository extends CrudRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUser(User user);

}
