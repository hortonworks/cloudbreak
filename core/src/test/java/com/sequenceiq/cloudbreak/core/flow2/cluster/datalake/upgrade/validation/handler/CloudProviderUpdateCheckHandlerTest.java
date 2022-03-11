package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class CloudProviderUpdateCheckHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudConnector<Object> cloudConnector;

    @InjectMocks
    private CloudProviderUpdateCheckHandler underTest;

    @Test
    void doAcceptShouldCallPlatformConnectorsGet() {
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        CloudStack cloudStack = new CloudStack(List.of(), null, null, Map.of(), Map.of(), "", null, "", "", null);
        CloudContext cloudContext = CloudContext.Builder.builder().withId(0L).build();
        ClusterUpgradeUpdateCheckRequest checkRequest = new ClusterUpgradeUpdateCheckRequest(0L, cloudStack, new CloudCredential(), cloudContext, List.of());

        underTest.doAccept(new HandlerEvent<>(new Event<>(checkRequest)));

        verify(cloudPlatformConnectors, times(1)).get(any(), any());
    }
}