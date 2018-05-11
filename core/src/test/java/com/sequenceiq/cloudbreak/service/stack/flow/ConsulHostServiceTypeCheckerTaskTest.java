package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.transport.TransportException;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class ConsulHostServiceTypeCheckerTaskTest {

    private static final String AMBARI_SERVICE = "ambari-8080";

    private static final String SERVICE_ENDPOINT = "/v1/catalog/service/";

    private static final String SERVICE_RESPONSE = "[{\"Node\":\"ip-10-0-0-124\",\"Address\":\"10.0.0.124\",\"ServiceID\":\"10.0.0.124:ambari:8080\","
            + "\"ServiceName\":\"ambari-8080\",\"ServiceTags\":null,\"ServicePort\":8080}]";

    @InjectMocks
    private ConsulServiceCheckerTask task;

    @Mock
    private Stack stack;

    @Test
    @SuppressWarnings("unchecked")
    public void checkStatusForConnectionError() {
        ConsulRawClient raw1 = mock(ConsulRawClient.class);
        ConsulClient client1 = new ConsulClient(raw1);
        when(raw1.makeGetRequest(SERVICE_ENDPOINT + AMBARI_SERVICE, null, QueryParams.DEFAULT)).thenThrow(TransportException.class);

        boolean result = task.checkStatus(new ConsulContext(stack, client1, Collections.singletonList(AMBARI_SERVICE)));

        assertFalse(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkStatusForOneNodeResponse() {
        ConsulRawClient raw1 = mock(ConsulRawClient.class);
        RawResponse rawResponse = new RawResponse(200, null, SERVICE_RESPONSE, null, null, null);
        ConsulClient client1 = new ConsulClient(raw1);
        when(raw1.makeGetRequest(SERVICE_ENDPOINT + AMBARI_SERVICE, null, QueryParams.DEFAULT)).thenReturn(rawResponse);

        boolean result = task.checkStatus(new ConsulContext(stack, client1, Collections.singletonList(AMBARI_SERVICE)));

        assertTrue(result);
    }

}
