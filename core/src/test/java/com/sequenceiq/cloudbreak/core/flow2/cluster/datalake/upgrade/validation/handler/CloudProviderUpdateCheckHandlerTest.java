package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLOUDPROVIDER_UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckRequest;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class CloudProviderUpdateCheckHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudConnector cloudConnector;

    @InjectMocks
    private CloudProviderUpdateCheckHandler underTest;

    @Test
    void doAcceptShouldCallPlatformConnectorsGet() {
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(cloudConnector);
        CloudStack cloudStack = CloudStack.builder()
                .build();
        CloudContext cloudContext = CloudContext.Builder.builder().withId(0L).build();
        ClusterUpgradeUpdateCheckRequest checkRequest = new ClusterUpgradeUpdateCheckRequest(0L, cloudStack, new CloudCredential(), cloudContext, List.of());

        underTest.doAccept(new HandlerEvent<>(new Event<>(checkRequest)));

        verify(cloudPlatformConnectors, times(1)).get(any(), any());
    }

    @Test
    void testSelector() {
        assertEquals(VALIDATE_CLOUDPROVIDER_UPDATE.selector(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        CloudStack cloudStack = CloudStack.builder()
                .build();
        CloudContext cloudContext = CloudContext.Builder.builder().withId(0L).build();
        ClusterUpgradeUpdateCheckRequest checkRequest = new ClusterUpgradeUpdateCheckRequest(0L, cloudStack, new CloudCredential(), cloudContext, List.of());
        Exception exception = new Exception("bumm");

        ClusterUpgradeUpdateCheckFailed result = (ClusterUpgradeUpdateCheckFailed) underTest.defaultFailureEvent(0L, exception, new Event<>(checkRequest));

        assertEquals(0L, result.getResourceId());
        assertEquals(cloudStack, result.getCloudStack());
        assertEquals(checkRequest.getCloudCredential(), result.getCloudCredential());
        assertEquals(cloudContext, result.getCloudContext());
        assertEquals(exception, result.getError());

    }
}
