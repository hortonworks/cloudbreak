package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;

@EntityType(entityClass = AccountPreferences.class)
public interface AccountPreferencesRepository extends CrudRepository<AccountPreferences, Long> {

    AccountPreferences findByAccount(@Param("account") String account);

}
