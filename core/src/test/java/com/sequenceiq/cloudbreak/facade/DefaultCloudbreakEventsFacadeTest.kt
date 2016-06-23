package com.sequenceiq.cloudbreak.facade

import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Date

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService

@RunWith(MockitoJUnitRunner::class)
class DefaultCloudbreakEventsFacadeTest {

    @Mock
    private val cloudbreakEventService: CloudbreakEventService? = null

    @Mock
    private val conversionService: ConversionService? = null

    @InjectMocks
    private val underTest: DefaultCloudbreakEventsFacade? = null

    @Test
    fun findUsagesForParametersConvertUsagesToJson() {
        val cloudbreakEvents = TestUtil.generateGcpCloudbreakEvents(10)
        `when`(cloudbreakEventService!!.cloudbreakEvents(anyString(), anyLong())).thenReturn(cloudbreakEvents)
        `when`(conversionService!!.convert(anyObject<Any>(), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).thenReturn(ArrayList<CloudbreakEventsJson>())

        underTest!!.retrieveEvents("owner", Date().time)

        verify(cloudbreakEventService, times(1)).cloudbreakEvents(anyString(), anyLong())
        verify(conversionService, times(1)).convert(anyObject<Any>(), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))
    }

}