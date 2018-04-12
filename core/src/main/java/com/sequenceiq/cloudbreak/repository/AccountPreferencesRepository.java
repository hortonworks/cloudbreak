package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

@EntityType(entityClass = AccountPreferences.class)
public interface AccountPreferencesRepository extends CrudRepository<AccountPreferences, Long> {

    @Query("SELECT ap FROM AccountPreferences ap WHERE ap.account= :account")
    AccountPreferences findByAccount(@Param("account") String account);

}
