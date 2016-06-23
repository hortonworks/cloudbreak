package com.sequenceiq.cloudbreak.service.account


import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.CbUser

interface AccountPreferencesService {

    fun save(accountPreferences: AccountPreferences): AccountPreferences

    fun saveOne(user: CbUser, accountPreferences: AccountPreferences): AccountPreferences

    operator fun get(id: Long?): AccountPreferences

    fun getByAccount(account: String): AccountPreferences

    fun getOneById(id: Long?, user: CbUser): AccountPreferences

    fun getOneByAccount(user: CbUser): AccountPreferences

    fun delete(user: CbUser)

}
