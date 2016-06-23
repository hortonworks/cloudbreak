package com.sequenceiq.cloudbreak.service.usages

import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify

import java.util.ArrayList
import java.util.Arrays
import java.util.Date

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.data.jpa.domain.Specification

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository

class DefaultCloudbreakUsagesRetrievalHostServiceTypeTest {

    @InjectMocks
    private var underTest: DefaultCloudbreakUsagesRetrievalService? = null

    private var filterParameters: CbUsageFilterParameters? = null

    private var usage: CloudbreakUsage? = null

    @Mock
    private val cloudbreakUsageRepository: CloudbreakUsageRepository? = null

    @Before
    fun setUp() {
        underTest = DefaultCloudbreakUsagesRetrievalService()
        filterParameters = CbUsageFilterParameters.Builder().setFilterEndDate(DUMMY_END_DATE).setAccount(DUMMY_ACCOUNT).setSince(DUMMY_SINCE).setOwner(DUMMY_OWNER).setRegion(DUMMY_REGION).setCloud(DUMMY_CLOUD).build()
        usage = CloudbreakUsage()
        usage!!.account = DUMMY_ACCOUNT
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testFindUsagesFor() {
        // GIVEN
        given<List>(cloudbreakUsageRepository!!.findAll(Matchers.any(Specification<Any>::class.java))).willReturn(Arrays.asList<CloudbreakUsage>(usage))
        // WHEN
        val result = underTest!!.findUsagesFor(filterParameters)
        // THEN
        Mockito.verify(cloudbreakUsageRepository, Mockito.times(1)).findAll(Matchers.any(Specification<Any>::class.java))
        assertEquals(usage, result[0])
    }

    @Test
    fun testFindUsagesForWhenUsagesNotFound() {
        // GIVEN
        given<List>(cloudbreakUsageRepository!!.findAll(Matchers.any(Specification<Any>::class.java))).willReturn(ArrayList<CloudbreakUsage>())
        // WHEN
        val result = underTest!!.findUsagesFor(filterParameters)
        // THEN
        Mockito.verify(cloudbreakUsageRepository, Mockito.times(1)).findAll(Matchers.any(Specification<Any>::class.java))
        assertEquals(0, result.size.toLong())
    }

    companion object {
        private val DUMMY_ACCOUNT = "account"
        private val DUMMY_SINCE = Date().time
        private val DUMMY_END_DATE = Date().time
        private val DUMMY_OWNER = "owner"
        private val DUMMY_REGION = "region"
        private val DUMMY_CLOUD = "GCP"
    }
}
