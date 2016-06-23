package com.sequenceiq.cloudbreak.service.account

import java.util.Collections
import java.util.concurrent.locks.Lock

import javax.inject.Inject

import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.google.common.util.concurrent.Striped
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.repository.AccountPreferencesRepository

@Service
class SimpleAccountPreferencesService : AccountPreferencesService {

    @Inject
    private val repository: AccountPreferencesRepository? = null

    private val locks = Striped.lazyWeakLock(STRIPES)

    override fun save(accountPreferences: AccountPreferences): AccountPreferences {
        return repository!!.save(accountPreferences)
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    override fun saveOne(user: CbUser, accountPreferences: AccountPreferences): AccountPreferences {
        accountPreferences.account = user.account
        return repository!!.save(accountPreferences)
    }

    override fun get(id: Long?): AccountPreferences {
        return repository!!.findOne(id)
    }

    override fun getByAccount(account: String): AccountPreferences {
        val lock = locks.get(account)
        lock.lock()
        try {
            var accountPreferences: AccountPreferences? = repository!!.findByAccount(account)
            if (accountPreferences == null) {
                accountPreferences = createDefaultAccountPreferences(account)
            }
            return accountPreferences
        } finally {
            lock.unlock()
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    override fun getOneById(id: Long?, user: CbUser): AccountPreferences {
        val accountPreferences = repository!!.findOne(id)
        if (!user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("AccountPreferences are only available for admin users!")
        } else if (accountPreferences == null) {
            throw BadRequestException(String.format("AccountPreferences could not find with id: %s", id))
        } else if (accountPreferences.account != user.account) {
            throw BadRequestException("AccountPreferences are only available for the owner admin user!")
        } else {
            return accountPreferences
        }
    }

    override fun getOneByAccount(user: CbUser): AccountPreferences {
        val account = user.account
        val lock = locks.get(account)
        lock.lock()
        try {
            var accountPreferences: AccountPreferences? = repository!!.findByAccount(account)
            if (accountPreferences == null) {
                accountPreferences = createDefaultAccountPreferences(account)
            }
            return accountPreferences
        } finally {
            lock.unlock()
        }
    }

    override fun delete(user: CbUser) {
        val preferences = getOneByAccount(user)
        repository!!.delete(preferences)
    }

    private fun createDefaultAccountPreferences(account: String): AccountPreferences {
        val defaultPreferences = AccountPreferences()
        defaultPreferences.account = account
        defaultPreferences.maxNumberOfClusters = ZERO
        defaultPreferences.maxNumberOfNodesPerCluster = ZERO
        defaultPreferences.maxNumberOfClustersPerUser = ZERO
        defaultPreferences.setAllowedInstanceTypes(emptyList<String>())
        defaultPreferences.clusterTimeToLive = ZERO
        defaultPreferences.userTimeToLive = ZERO
        return repository!!.save(defaultPreferences)
    }

    companion object {
        private val ZERO = 0L
        private val STRIPES = 10
    }
}
