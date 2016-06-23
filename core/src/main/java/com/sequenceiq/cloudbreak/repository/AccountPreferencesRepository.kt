package com.sequenceiq.cloudbreak.repository

import com.sequenceiq.cloudbreak.domain.AccountPreferences
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

@EntityType(entityClass = AccountPreferences::class)
interface AccountPreferencesRepository : CrudRepository<AccountPreferences, Long> {

    fun findByAccount(@Param("account") account: String): AccountPreferences

}
