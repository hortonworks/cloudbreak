package com.sequenceiq.cloudbreak.cloud.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequestV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResultV2;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.eventbus.Event;

@ExtendWith(MockitoExtension.class)
class GetRegionsV2HandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private CloudConnector cloudConnector;

    @InjectMocks
    private GetRegionsV2Handler underTest;

    @Test
    void acceptReturnsGenericMessageForUnexpectedFailures() throws Exception {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(any(), any(), any(), eq(false)))
                .thenThrow(new NullPointerException("Cannot invoke \"java.util.Map.get(Object)\""));

        CloudCredential cloudCredential = new CloudCredential("id", "name", "account");
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                cloudCredential, "aws", "desc", "account", new ArrayList<>());
        GetPlatformRegionsRequestV2 request = new GetPlatformRegionsRequestV2(
                cloudCredential, extendedCloudCredential, "native", "region", null, false);

        underTest.accept(new Event<>(request));

        GetPlatformRegionsResultV2 result = request.await(5, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.getStatusReason()).isEqualTo("Failed to get regions from the cloud provider.");
        assertThat(result.getErrorDetails()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptReturnsGenericMessageForCloudConnectorFailures() throws Exception {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(any(), any(), any(), eq(false)))
                .thenThrow(new CloudConnectorException("The security token included in the request is invalid."));

        CloudCredential cloudCredential = new CloudCredential("id", "name", "account");
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                cloudCredential, "aws", "desc", "account", new ArrayList<>());
        GetPlatformRegionsRequestV2 request = new GetPlatformRegionsRequestV2(
                cloudCredential, extendedCloudCredential, "native", "region", null, false);

        underTest.accept(new Event<>(request));

        GetPlatformRegionsResultV2 result = request.await(5, TimeUnit.SECONDS);
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(result.getStatusReason()).isEqualTo("Failed to get regions from the cloud provider.");
        assertThat(result.getErrorDetails()).isInstanceOf(CloudConnectorException.class);
    }
}
