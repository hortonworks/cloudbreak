package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.audit.model.AuditEventName.AUTOSCALE_DATAHUB_CLUSTER;
import static com.sequenceiq.cloudbreak.audit.model.AuditEventName.MANAGE_AUTOSCALE_DATAHUB_CLUSTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.AuditEvent;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.periscope.api.model.ScalingStatus;

public class AuditServiceTest {

    @InjectMocks
    AuditService underTest;

    @Mock
    AuditClient auditClient;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAuditRestEvent() {
        Map requestParams = Map.of("param1", "param2");
        String userCrn = "crn:altus:iam:us-west-1:05681f13-41fc-4b2a-9588-a78d640f3c23:user:5f03bc0d-83d5-40c3-8624-f7b21481c1f7";
        underTest.auditRestApi(requestParams, true, "user-agent", userCrn, "user-account", "127.0.0.1");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditClient, times(1)).createAuditEvent(captor.capture());
        AuditEvent auditEvent = captor.getValue();

        assertEquals("user-account", auditEvent.getAccountId());
        assertEquals("127.0.0.1", auditEvent.getSourceIp());
        assertEquals(MANAGE_AUTOSCALE_DATAHUB_CLUSTER, auditEvent.getEventName());
        assertEquals(Crn.Service.DATAHUB, auditEvent.getEventSource());
        assertEquals(userCrn, ((ActorCrn) auditEvent.getActor()).getActorCrn());

        ApiRequestData apiRequestData = (ApiRequestData) auditEvent.getEventData();
        assertEquals(new Gson().toJson(requestParams), apiRequestData.getRequestParameters());
        assertEquals("user-agent", apiRequestData.getUserAgent());
        assertEquals(true, apiRequestData.isMutating());
    }

    @Test
    public void testAuditServiceEvent() {
        String auditMessage = "audit service message";
        underTest.auditAutoscaleServiceEvent(ScalingStatus.SUCCESS, auditMessage,  "clusterCrn",
                "user-account", System.currentTimeMillis());

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditClient, times(1)).createAuditEvent(captor.capture());
        AuditEvent auditEvent = captor.getValue();

        assertEquals("user-account", auditEvent.getAccountId());
        assertEquals(AUTOSCALE_DATAHUB_CLUSTER, auditEvent.getEventName());
        assertEquals(Crn.Service.DATAHUB, auditEvent.getEventSource());
        assertEquals(Crn.Service.DATAHUB.getName(), ((ActorService) auditEvent.getActor()).getActorServiceName());

        ServiceEventData serviceEventData = (ServiceEventData) auditEvent.getEventData();
        assertNotNull("Event Data should be intialized", serviceEventData.getEventDetails());
        assertTrue("Time stamp should be intialized", serviceEventData.getEventDetails().contains("timestamp"));
    }
}
