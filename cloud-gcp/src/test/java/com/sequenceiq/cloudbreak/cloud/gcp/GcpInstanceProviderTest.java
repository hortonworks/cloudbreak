package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;

@ExtendWith(MockitoExtension.class)
class GcpInstanceProviderTest {

    private static final String AZ = "europe-north1";

    private static final String INSTANCE_NAME_PREFIX = "testcluster";

    private static final String INSTANCE_NAME_1 = "testcluster-w-1";

    private static final String INSTANCE_NAME_2 = "testcluster-w-2";

    private static final String INSTANCE_NAME_3 = "testcluster-w-3";

    private static final String FIRST_PAGE_TOKEN = "0";

    private static final String SECOND_PAGE_TOKEN = "1";

    @InjectMocks
    private GcpInstanceProvider underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Instances instancesMock;

    @Mock
    private Compute.Instances.List computeInstancesMock;

    @Mock
    private GcpStackUtil gcpStackUtil;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void before() throws IOException {
        authenticatedContext = createAuthenticatedContext();
        when(compute.instances()).thenReturn(instancesMock);
        when(instancesMock.list(any(), eq(AZ))).thenReturn(computeInstancesMock);
        when(computeInstancesMock.setFilter(anyString())).thenReturn(computeInstancesMock);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("test");
    }

    @Test
    public void testProvideShouldReturnsInstances() throws IOException {
        InstanceList gcpInstancesFirstPage = createGcpInstancesFirstPage();
        InstanceList gcpInstancesSecondPage = createGcpInstancesSecondPage();
        InstanceList gcpInstancesThirdPage = createGcpInstancesThirdPage();

        when(computeInstancesMock.execute())
                .thenReturn(gcpInstancesFirstPage)
                .thenReturn(gcpInstancesSecondPage)
                .thenReturn(gcpInstancesThirdPage);

        List<Instance> actual = underTest.getInstances(authenticatedContext, INSTANCE_NAME_PREFIX);

        assertEquals(3, actual.size());
        assertEquals(gcpInstancesFirstPage.getItems().get(0), actual.get(0));
        assertEquals(gcpInstancesSecondPage.getItems().get(0), actual.get(1));
        assertEquals(gcpInstancesThirdPage.getItems().get(0), actual.get(2));
    }

    @Test
    public void testProvideShouldReturnsEmptyListWhenThereAreNoResponseFromGcp() throws IOException {
        when(computeInstancesMock.execute()).thenReturn(new InstanceList());

        List<Instance> actual = underTest.getInstances(authenticatedContext, INSTANCE_NAME_PREFIX);

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testProvideShouldReturnsEmptyListWhenTheGcpThrowsException() throws IOException {
        when(computeInstancesMock.execute()).thenThrow(new IOException("Error happened on GCP side."));

        List<Instance> actual = underTest.getInstances(authenticatedContext, INSTANCE_NAME_PREFIX);

        assertTrue(actual.isEmpty());
    }

    private AuthenticatedContext createAuthenticatedContext() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential("1", "gcp-cred", Collections.singletonMap("projectId", "gcp-cred"), "acc");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(null, AvailabilityZone.availabilityZone(AZ));
        return CloudContext.Builder.builder()
                .withName("test-cluster")
                .withLocation(location)
                .withUserName("")
                .build();
    }

    private InstanceList createGcpInstancesFirstPage() {
        InstanceList instanceList = new InstanceList();
        instanceList.setItems(List.of(createInstance(INSTANCE_NAME_1)));
        instanceList.setNextPageToken(FIRST_PAGE_TOKEN);
        return instanceList;
    }

    private InstanceList createGcpInstancesSecondPage() {
        InstanceList instanceList = new InstanceList();
        instanceList.setItems(List.of(createInstance(INSTANCE_NAME_2)));
        instanceList.setNextPageToken(SECOND_PAGE_TOKEN);
        return instanceList;
    }

    private InstanceList createGcpInstancesThirdPage() {
        InstanceList instanceList = new InstanceList();
        instanceList.setItems(List.of(createInstance(INSTANCE_NAME_3)));
        instanceList.setNextPageToken(null);
        return instanceList;
    }

    private Instance createInstance(String name) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }
}