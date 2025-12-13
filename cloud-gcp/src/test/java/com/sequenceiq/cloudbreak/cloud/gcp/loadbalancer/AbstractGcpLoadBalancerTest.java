package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class AbstractGcpLoadBalancerTest {

    @InjectMocks
    private GcpHealthCheckResourceBuilder underTest;

    @Test
    public void testCheckResources() {
        Compute compute = mock(Compute.class);
        GcpContext gcpContext = new GcpContext(
                "name",
                location(region("location")),
                null,
                "serviceAccountId",
                compute,
                false,
                1,
                true);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);
        List<CloudResource> resourceList = new ArrayList<>();
        List<CloudResourceStatus> cloudResourceStatuses = underTest.checkResources(gcpContext, authenticatedContext, resourceList);
        assertEquals(true, cloudResourceStatuses.isEmpty());
    }
}
