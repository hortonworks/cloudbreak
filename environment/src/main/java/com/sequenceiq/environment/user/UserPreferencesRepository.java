package com.sequenceiq.environment.user;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

@Transactional(TxType.REQUIRED)
public interface UserPreferencesRepository extends CrudRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserCrn(String userCrn);

}
