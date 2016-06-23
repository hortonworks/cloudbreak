package com.sequenceiq.cloudbreak.service.account

import java.util.Calendar

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

@Component
class AccountPreferencesValidator {

    @Inject
    private val accountPreferencesService: AccountPreferencesService? = null

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val userDetailsService: UserDetailsService? = null

    @Throws(AccountPreferencesValidationFailed::class)
    fun validate(stack: Stack, account: String, owner: String) {
        validate(stack.instanceGroups, stack.fullNodeCount, account, owner)
    }

    @Throws(AccountPreferencesValidationFailed::class)
    fun validate(stackId: Long?, scalingAdjustment: Int?) {
        val stack = stackService!!.getById(stackId)
        val newNodeCount = stack.fullNodeCount!! + scalingAdjustment!!
        validate(stack.instanceGroups, newNodeCount, stack.account, stack.owner)
    }

    @Throws(AccountPreferencesValidationFailed::class)
    private fun validate(instanceGroups: Set<InstanceGroup>, nodeCount: Int?, account: String, owner: String) {
        val preferences = accountPreferencesService!!.getByAccount(account)
        validateNumberOfNodesPerCluster(nodeCount, preferences)
        validateNumberOfClusters(account, preferences)
        validateAllowedInstanceTypes(instanceGroups, preferences)
        validateNumberOfClustersPerUser(owner, preferences)
        validateUserTimeToLive(owner, preferences)
    }

    @Throws(AccountPreferencesValidationFailed::class)
    private fun validateNumberOfNodesPerCluster(nodeCount: Int?, preferences: AccountPreferences) {
        val maxNodeNumberPerCluster = preferences.maxNumberOfNodesPerCluster
        if (needToValidateField(maxNodeNumberPerCluster!!) && nodeCount > maxNodeNumberPerCluster) {
            throw AccountPreferencesValidationFailed(String.format("Cluster with maximum '%s' instances could be created within this account!",
                    maxNodeNumberPerCluster))
        }
    }

    @Throws(AccountPreferencesValidationFailed::class)
    private fun validateNumberOfClusters(account: String, preferences: AccountPreferences) {
        val maxNumberOfClusters = preferences.maxNumberOfClusters
        if (needToValidateField(maxNumberOfClusters!!)) {
            val stacks = stackService!!.retrieveAccountStacks(account)
            if (stacks.size >= maxNumberOfClusters) {
                throw AccountPreferencesValidationFailed(
                        String.format("No more cluster could be created! The number of clusters exceeded the account's limit(%s)!", maxNumberOfClusters))
            }
        }
    }

    @Throws(AccountPreferencesValidationFailed::class)
    private fun validateAllowedInstanceTypes(instanceGroups: Set<InstanceGroup>, preferences: AccountPreferences) {
        val allowedInstanceTypes = preferences.allowedInstanceTypes
        if (needToValidateField(allowedInstanceTypes)) {
            for (ig in instanceGroups) {
                val instanceTypeName = ig.template.instanceType
                if (!allowedInstanceTypes.contains(instanceTypeName)) {
                    throw AccountPreferencesValidationFailed(
                            String.format("The '%s' instance type isn't allowed within the account!", instanceTypeName))
                }
            }
        }
    }

    @Throws(AccountPreferencesValidationFailed::class)
    private fun validateNumberOfClustersPerUser(owner: String, preferences: AccountPreferences) {
        val maxClustersPerUser = preferences.maxNumberOfClustersPerUser
        if (needToValidateField(maxClustersPerUser!!)) {
            val stacks = stackService!!.retrieveOwnerStacks(owner)
            if (stacks.size >= maxClustersPerUser) {
                throw AccountPreferencesValidationFailed(
                        String.format("No more cluster could be created! The number of clusters exceeded the user's limit(%s)!", maxClustersPerUser))
            }
        }
    }

    @Throws(AccountPreferencesValidationFailed::class)
    fun validateUserTimeToLive(owner: String, preferences: AccountPreferences) {
        val userTimeToLive = preferences.userTimeToLive
        if (needToValidateField(userTimeToLive!!)) {
            val cbUser = userDetailsService!!.getDetails(owner, UserFilterField.USERID)
            val now = Calendar.getInstance().timeInMillis
            val userActiveTime = now - cbUser.created.time
            if (userActiveTime > userTimeToLive) {
                throw AccountPreferencesValidationFailed("The user demo time is expired!")
            }
        }
    }

    @Throws(AccountPreferencesValidationFailed::class)
    fun validateClusterTimeToLive(created: Long?, preferences: AccountPreferences) {
        val clusterTimeToLive = preferences.clusterTimeToLive
        if (needToValidateField(clusterTimeToLive!!)) {
            val now = Calendar.getInstance().timeInMillis
            val clusterRunningTime = now - created!!
            if (clusterRunningTime > clusterTimeToLive) {
                throw AccountPreferencesValidationFailed("The maximum running time that is configured for the account is exceeded by the cluster!")
            }
        }
    }

    private fun needToValidateField(field: Long): Boolean {
        return EXTREMAL_VALUE != field
    }

    private fun needToValidateField(field: List<String>): Boolean {
        return !field.isEmpty()
    }

    companion object {
        private val EXTREMAL_VALUE = 0L
    }
}