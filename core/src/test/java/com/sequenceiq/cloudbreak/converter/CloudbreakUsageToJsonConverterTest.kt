package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP
import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyString

import java.util.Arrays
import java.util.Date

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

class CloudbreakUsageToJsonConverterTest : AbstractEntityConverterTest<CloudbreakUsage>() {

    @InjectMocks
    private var underTest: CloudbreakUsageToJsonConverter? = null

    @Mock
    private val userDetailsService: UserDetailsService? = null

    private var user: CbUser? = null

    @Before
    fun setUp() {
        underTest = CloudbreakUsageToJsonConverter()
        user = createCbUser()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).willReturn(user)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(GCP, result.provider)
        assertEquals("john.smith@example.com", result.username)
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone"))
    }

    @Test
    fun testConvertWithAwsProvider() {
        // GIVEN
        source.provider = AWS
        source.region = "us_east_1"
        given(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).willReturn(user)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(AWS, result.provider)
        assertEquals("john.smith@example.com", result.username)
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone"))
    }

    @Test
    fun testConvertWithGcpProvider() {
        // GIVEN
        source.provider = GCP
        source.region = "us_central1"
        source.availabilityZone = "us_central1_a"
        given(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).willReturn(user)
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(GCP, result.provider)
        assertEquals("john.smith@example.com", result.username)
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone"))
    }

    override fun createSource(): CloudbreakUsage {
        return TestUtil.gcpCloudbreakUsage(1L)
    }

    private fun createCbUser(): CbUser {
        return CbUser("dummyUserId", "john.smith@example.com", "dummyAccount",
                Arrays.asList(CbUserRole.ADMIN, CbUserRole.USER), "John", "Smith", Date())
    }
}
