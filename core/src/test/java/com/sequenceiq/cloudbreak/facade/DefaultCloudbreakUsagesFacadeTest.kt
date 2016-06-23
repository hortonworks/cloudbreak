package com.sequenceiq.cloudbreak.facade

import org.mockito.Matchers.any
import org.mockito.Matchers.anyObject
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.ArrayList

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService

@RunWith(MockitoJUnitRunner::class)
class DefaultCloudbreakUsagesFacadeTest {

    @Mock
    private val cloudbreakUsagesService: CloudbreakUsagesRetrievalService? = null

    @Mock
    private val cloudbreakUsageGeneratorService: CloudbreakUsageGeneratorService? = null

    @Mock
    private val conversionService: ConversionService? = null

    @InjectMocks
    private val underTest: DefaultCloudbreakUsagesFacade? = null

    @Test
    fun findUsagesForParametersConvertUsagesToJson() {
        val cloudbreakUsages = TestUtil.generateAzureCloudbreakUsages(10)
        `when`(cloudbreakUsagesService!!.findUsagesFor(any<CbUsageFilterParameters>(CbUsageFilterParameters::class.java))).thenReturn(cloudbreakUsages)
        `when`(conversionService!!.convert(anyObject<Any>(), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).thenReturn(ArrayList<CloudbreakUsageJson>())

        underTest!!.getUsagesFor(CbUsageFilterParameters.Builder().build())

        verify(cloudbreakUsagesService, times(1)).findUsagesFor(any<CbUsageFilterParameters>(CbUsageFilterParameters::class.java))
        verify(conversionService, times(1)).convert(anyObject<Any>(), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))
    }

    @Test
    fun generateUserUsagesCallWithoutError() {
        doNothing().`when`<CloudbreakUsageGeneratorService>(cloudbreakUsageGeneratorService).generate()

        underTest!!.generateUserUsages()

        verify<CloudbreakUsageGeneratorService>(cloudbreakUsageGeneratorService, times(1)).generate()
    }
}