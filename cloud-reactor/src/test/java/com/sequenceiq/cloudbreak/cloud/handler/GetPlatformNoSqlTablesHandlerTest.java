package com.sequenceiq.cloudbreak.cloud.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.eventbus.Event;

@ExtendWith(MockitoExtension.class)
class GetPlatformNoSqlTablesHandlerTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private CloudConnector cloudConnector;

    @InjectMocks
    private GetPlatformNoSqlTablesHandler underTest;

    @Test
    void type() {
        assertEquals(GetPlatformNoSqlTablesRequest.class, underTest.type());
    }

    @Test
    void accept() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);

        CloudCredential cloudCredential = new CloudCredential("id", "name", "acc");
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                cloudCredential, "aws", "desc", "crn", "account", new ArrayList<>());
        GetPlatformNoSqlTablesRequest request = spy(new GetPlatformNoSqlTablesRequest(cloudCredential,
                extendedCloudCredential,
                "aws",
                "region",
                null));

        underTest.accept(new Event<>(request));

        verify(platformResources).noSqlTables(eq(extendedCloudCredential), eq(Region.region("region")), isNull());
        verify(request).getResult();
    }
}
