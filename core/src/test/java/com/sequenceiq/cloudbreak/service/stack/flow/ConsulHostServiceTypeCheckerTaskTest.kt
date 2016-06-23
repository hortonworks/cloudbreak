package com.sequenceiq.cloudbreak.service.stack.flow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import java.net.ConnectException
import java.util.Arrays

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.ecwid.consul.transport.RawResponse
import com.ecwid.consul.v1.ConsulClient
import com.ecwid.consul.v1.ConsulRawClient
import com.ecwid.consul.v1.QueryParams
import com.sequenceiq.cloudbreak.domain.Stack

@RunWith(MockitoJUnitRunner::class)
class ConsulHostServiceTypeCheckerTaskTest {

    @InjectMocks
    private val task: ConsulServiceCheckerTask? = null

    @Mock
    private val stack: Stack? = null

    @Test
    @SuppressWarnings("unchecked")
    fun checkStatusForConnectionError() {
        val raw1 = mock<ConsulRawClient>(ConsulRawClient::class.java)
        val client1 = ConsulClient(raw1)
        `when`(raw1.makeGetRequest(SERVICE_ENDPOINT + AMBARI_SERVICE, null, QueryParams.DEFAULT)).thenThrow(ConnectException::class.java)

        val result = task!!.checkStatus(ConsulContext(stack, client1, Arrays.asList(AMBARI_SERVICE)))

        assertFalse(result)
    }

    @Test
    @SuppressWarnings("unchecked")
    fun checkStatusForOneNodeResponse() {
        val raw1 = mock<ConsulRawClient>(ConsulRawClient::class.java)
        val rawResponse = RawResponse(200, null, SERVICE_RESPONSE, null, null, null)
        val client1 = ConsulClient(raw1)
        `when`(raw1.makeGetRequest(SERVICE_ENDPOINT + AMBARI_SERVICE, null, QueryParams.DEFAULT)).thenReturn(rawResponse)

        val result = task!!.checkStatus(ConsulContext(stack, client1, Arrays.asList(AMBARI_SERVICE)))

        assertTrue(result)
    }

    companion object {

        private val AMBARI_SERVICE = "ambari-8080"
        private val SERVICE_ENDPOINT = "/v1/catalog/service/"
        private val SERVICE_RESPONSE = "[{\"Node\":\"ip-10-0-0-124\",\"Address\":\"10.0.0.124\",\"ServiceID\":\"10.0.0.124:ambari:8080\"," + "\"ServiceName\":\"ambari-8080\",\"ServiceTags\":null,\"ServicePort\":8080}]"
    }

}
