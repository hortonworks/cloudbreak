package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;

@EntityType(entityClass = AccountPreferences.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface AccountPreferencesRepository extends CrudRepository<AccountPreferences, Long> {

    @Query("SELECT ap FROM AccountPreferences ap WHERE ap.account= :account")
    AccountPreferences findByAccount(@Param("account") String account);

}
