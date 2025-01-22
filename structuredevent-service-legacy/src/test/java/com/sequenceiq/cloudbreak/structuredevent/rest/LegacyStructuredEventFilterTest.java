package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.REQUEST_TIME;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_DETAILS;
import static com.sequenceiq.cloudbreak.structuredevent.filter.CDPJaxRsFilterPropertyKeys.RESPONSE_LOGGING_STREAM;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;

@ExtendWith(MockitoExtension.class)
class LegacyStructuredEventFilterTest {

    @Mock
    private WriterInterceptorContext context;

    @Mock
    private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private LegacyDefaultStructuredEventClient legacyStructuredEventClient;

    @Mock
    private NodeConfig nodeConfig;

    @InjectMocks
    private LegacyStructuredEventFilter underTest;

    @Test
    void testAroundWriteTo() throws IOException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.1");
        when(nodeConfig.getId()).thenReturn("1");
        when(context.getProperty("structuredevent.loggingEnabled")).thenReturn(true);
        when(context.getProperty(REQUEST_TIME)).thenReturn(1L);
        when(context.getProperty(REQUEST_DETAILS)).thenReturn(new RestRequestDetails());
        when(context.getProperty(RESPONSE_DETAILS)).thenReturn(new RestResponseDetails());
        when(context.getProperty(RESPONSE_LOGGING_STREAM)).thenReturn(null);
        when(context.getProperty("REST_PARAMS")).thenReturn(Map.of());

        underTest.aroundWriteTo(context);

        verify(cloudbreakRestRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(cloudbreakRestRequestThreadLocalService, times(1)).getRequestedWorkspaceId();
    }

}
