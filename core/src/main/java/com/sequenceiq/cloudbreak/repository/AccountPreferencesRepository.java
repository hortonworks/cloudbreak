package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@EntityType(entityClass = AccountPreferences.class)
public interface AccountPreferencesRepository extends CrudRepository<AccountPreferences, Long> {

    AccountPreferences findByAccount(@Param("account") String account);

}
