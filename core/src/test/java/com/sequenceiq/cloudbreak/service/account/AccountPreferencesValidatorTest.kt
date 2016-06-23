package com.sequenceiq.cloudbreak.service.account

import org.mockito.Matchers.any
import org.mockito.Matchers.anyString
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

@RunWith(MockitoJUnitRunner::class)
class AccountPreferencesValidatorTest {
    @Mock
    private val stack: Stack? = null

    @Mock
    private val preferences: AccountPreferences? = null

    @Mock
    private val accountPreferencesService: AccountPreferencesService? = null

    @Mock
    private val stackService: StackService? = null

    @Mock
    private val userDetailsService: UserDetailsService? = null

    @InjectMocks
    private val underTest: AccountPreferencesValidator? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        `when`(stack!!.account).thenReturn("")
        `when`(accountPreferencesService!!.getByAccount("")).thenReturn(preferences)
        `when`(preferences!!.maxNumberOfNodesPerCluster).thenReturn(0L)
        `when`(preferences.maxNumberOfClusters).thenReturn(0L)
        `when`(preferences.maxNumberOfClustersPerUser).thenReturn(0L)
        `when`(preferences.clusterTimeToLive).thenReturn(0L)
        `when`(preferences.userTimeToLive).thenReturn(0L)
        `when`(preferences.allowedInstanceTypes).thenReturn(ArrayList<String>())
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenPreferencesShouldNotBeValidated() {
        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test(expected = AccountPreferencesValidationFailed::class)
    @Throws(Exception::class)
    fun testValidateShouldThrowExceptionWhenTheStackNodeCountIsGreaterThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfNodesPerCluster).thenReturn(4L)
        `when`(stack!!.fullNodeCount).thenReturn(5)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenTheStackNodeCountIsLessThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfNodesPerCluster).thenReturn(4L)
        `when`(stack!!.fullNodeCount).thenReturn(3)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test(expected = AccountPreferencesValidationFailed::class)
    @Throws(Exception::class)
    fun testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountIsGreaterOrEqualThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfClusters).thenReturn(400L)
        val stacks = Mockito.mock<Set<Any>>(Set<Any>::class.java)
        `when`(stackService!!.retrieveAccountStacks(anyString())).thenReturn(stacks)
        `when`(stacks.size).thenReturn(400)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountIsLessThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfClusters).thenReturn(400L)
        val stacks = Mockito.mock<Set<Any>>(Set<Any>::class.java)
        `when`(stackService!!.retrieveAccountStacks(anyString())).thenReturn(stacks)
        `when`(stacks.size).thenReturn(200)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test(expected = AccountPreferencesValidationFailed::class)
    @Throws(Exception::class)
    fun testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsGreaterOrEqualThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfClustersPerUser).thenReturn(4L)
        val stacks = Mockito.mock<Set<Any>>(Set<Any>::class.java)
        `when`(stackService!!.retrieveOwnerStacks(anyString())).thenReturn(stacks)
        `when`(stacks.size).thenReturn(4)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsLessThanTheAccountMaximum() {
        `when`(preferences!!.maxNumberOfClustersPerUser).thenReturn(4L)
        val stacks = Mockito.mock<Set<Any>>(Set<Any>::class.java)
        `when`(stackService!!.retrieveOwnerStacks(anyString())).thenReturn(stacks)
        `when`(stacks.size).thenReturn(2)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test(expected = AccountPreferencesValidationFailed::class)
    @Throws(Exception::class)
    fun testValidateShouldThrowExceptionWhenTheUserDemoTimeExpired() {
        val calendar = Calendar.getInstance()
        calendar.roll(Calendar.HOUR_OF_DAY, -1)
        `when`(preferences!!.userTimeToLive).thenReturn(40000L)
        val cbUser = Mockito.mock<CbUser>(CbUser::class.java)
        `when`(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).thenReturn(cbUser)
        `when`(cbUser.created).thenReturn(calendar.time)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenTheUserDemoTimeHasNotExpiredYet() {
        val calendar = Calendar.getInstance()
        calendar.roll(Calendar.MINUTE, -1)
        `when`(preferences!!.userTimeToLive).thenReturn(65000L)
        val cbUser = Mockito.mock<CbUser>(CbUser::class.java)
        `when`(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).thenReturn(cbUser)
        `when`(cbUser.created).thenReturn(calendar.time)

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test(expected = AccountPreferencesValidationFailed::class)
    @Throws(Exception::class)
    fun testValidateShouldThrowExceptionWhenTheStackContainsNotAllowedInstanceTypes() {
        val n1St4Type = "n1-standard-4"
        val allowedInstanceTypes = Arrays.asList(n1St4Type, "n1-standard-8", "n1-standard-16")
        `when`(preferences!!.allowedInstanceTypes).thenReturn(allowedInstanceTypes)
        val cbgateway = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        val master = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        val slave = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        `when`(cbgateway.template.instanceType).thenReturn(n1St4Type)
        `when`(master.template.instanceType).thenReturn(n1St4Type)
        `when`(slave.template.instanceType).thenReturn("n1-standard-32")
        `when`(stack!!.instanceGroups).thenReturn(Sets.newHashSet(cbgateway, master, slave))

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    @Test
    @Throws(Exception::class)
    fun testValidateShouldNotThrowExceptionWhenTheStackContainsOnlyAllowedInstanceTypes() {
        val n1St4Type = "n1-standard-4"
        val n1St6Type = "n1-standard-8"
        val allowedInstanceTypes = Arrays.asList(n1St4Type, n1St6Type, "n1-standard-16")
        `when`(preferences!!.allowedInstanceTypes).thenReturn(allowedInstanceTypes)
        val cbgateway = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        val master = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        val slave = Mockito.mock<InstanceGroup>(InstanceGroup::class.java, Mockito.RETURNS_DEEP_STUBS)
        `when`(cbgateway.template.instanceType).thenReturn(n1St4Type)
        `when`(master.template.instanceType).thenReturn(n1St4Type)
        `when`(slave.template.instanceType).thenReturn(n1St6Type)
        `when`(stack!!.instanceGroups).thenReturn(Sets.newHashSet(cbgateway, master, slave))

        underTest!!.validate(stack, EMPTY_STRING, EMPTY_STRING)
    }

    companion object {

        val EMPTY_STRING = ""
    }
}