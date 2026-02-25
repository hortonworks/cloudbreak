package com.sequenceiq.cloudbreak.cloud.gcp.group;

import static com.google.api.client.http.HttpResponseException.Builder;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.api.type.ResourceType.GCP_FIREWALL_IN;
import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.GcpComputeResourceChecker;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AbstractGcpGroupBuilderTest {

    @Mock
    private GcpResourceNameService resourceNameService;

    @Mock
    private GcpComputeResourceChecker resourceChecker;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private TestGcpGroupBuilder underTest;

    @Test
    void testCheckResources() {
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

    @Test
    void whenGcpInstanceGroupRequestReturnsHttp409ThenExecuteOperationalRequestReturnsCloudResource() throws IOException {
        CloudResource cloudResource = CloudResource.builder().withType(GCP_INSTANCE_GROUP).withName("group1").build();
        ComputeRequest computeRequest = mock(ComputeRequest.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonError.getCode()).thenReturn(HttpStatus.SC_CONFLICT);
        when(computeRequest.execute()).thenThrow(new GoogleJsonResponseException(new Builder(409, "conflict", new HttpHeaders()),
                googleJsonError));
        CloudResource createdCloudResource = underTest.executeOperationalRequest(cloudResource, computeRequest);
        assertEquals("group1", createdCloudResource.getName());
        assertEquals(GCP_INSTANCE_GROUP, createdCloudResource.getType());
    }

    @Test
    void whenNotGcpInstanceGroupRequestThenExecuteOperationalRequestThrowsError() throws IOException {
        CloudResource cloudResource = CloudResource.builder().withType(GCP_FIREWALL_IN).withName("firewall").build();
        ComputeRequest computeRequest = mock(ComputeRequest.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);
        when(googleJsonError.getCode()).thenReturn(HttpStatus.SC_CONFLICT);
        when(googleJsonError.getMessage()).thenReturn("error");
        when(computeRequest.execute()).thenThrow(new GoogleJsonResponseException(new Builder(409, "conflict", new HttpHeaders()),
                googleJsonError));
        GcpResourceException gcpResourceException = Assertions.assertThrows(GcpResourceException.class,
                () -> underTest.executeOperationalRequest(cloudResource, computeRequest));
        assertEquals("error", gcpResourceException.getMessage());
    }
}