package com.sequenceiq.cloudbreak.service.account

import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService

@Service
class ScheduledAccountPreferencesValidator {

    @Inject
    private val accountPreferencesService: AccountPreferencesService? = null

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val preferencesValidator: AccountPreferencesValidator? = null

    @Inject
    private val flowManager: ReactorFlowManager? = null

    @Scheduled(cron = EVERY_HOUR_0MIN_0SEC)
    fun validate() {
        LOGGER.info("Validate account preferences for all 'running' stack.")
        val accountPreferences = HashMap<String, AccountPreferences>()
        val allAlive = stackService!!.allAlive

        for (stack in allAlive) {
            val preferences = getAccountPreferences(stack.account, accountPreferences)
            try {
                preferencesValidator!!.validateClusterTimeToLive(stack.created, preferences)
                preferencesValidator.validateUserTimeToLive(stack.owner, preferences)
            } catch (e: AccountPreferencesValidationFailed) {
                terminateStack(stack)
            }

        }
    }

    private fun getAccountPreferences(account: String, accountPreferences: MutableMap<String, AccountPreferences>): AccountPreferences {
        if (accountPreferences.containsKey(account)) {
            return accountPreferences[account]
        } else {
            val preferences = accountPreferencesService!!.getByAccount(account)
            accountPreferences.put(account, preferences)
            return preferences
        }
    }

    private fun terminateStack(stack: Stack) {
        if (!stack.isDeleteCompleted) {
            LOGGER.info("Trigger termination of stack: '{}', owner: '{}', account: '{}'.", stack.name, stack.owner, stack.account)
            flowManager!!.triggerTermination(stack.id)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScheduledAccountPreferencesValidator::class.java)
        private val EVERY_HOUR_0MIN_0SEC = "0 0 * * * *"
    }
}
