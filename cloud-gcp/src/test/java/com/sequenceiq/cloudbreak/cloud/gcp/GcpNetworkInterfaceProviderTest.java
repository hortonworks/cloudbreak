package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpNetworkInterfaceProviderTest {

    private static final String AZ = "europe-north1";

    private static final String INSTANCE_NAME_1 = "testcluster-w-1";

    private static final String INSTANCE_NAME_2 = "testcluster-w-2";

    private static final String INSTANCE_NAME_3 = "testcluster-w-3";

    private static final String FIRST_PAGE_TOKEN = "0";

    private static final String SECOND_PAGE_TOKEN = "1";

    @InjectMocks
    private GcpNetworkInterfaceProvider underTest;

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

    private List<CloudResource> instances;

    @BeforeEach
    public void before() throws IOException {
        authenticatedContext = createAuthenticatedContext();
        instances = createCloudResources();
        when(compute.instances()).thenReturn(instancesMock);
        when(instancesMock.list(any(), eq(AZ))).thenReturn(computeInstancesMock);
        when(computeInstancesMock.setFilter(anyString())).thenReturn(computeInstancesMock);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("test");
    }

    @Test
    public void testProvideShouldReturnsTheNetworkInterfaces() throws IOException {
        InstanceList gcpInstancesFirstPage = createGcpInstancesFirstPage();
        InstanceList gcpInstancesSecondPage = createGcpInstancesSecondPage();
        InstanceList gcpInstancesThirdPage = createGcpInstancesThirdPage();

        when(computeInstancesMock.execute())
                .thenReturn(gcpInstancesFirstPage)
                .thenReturn(gcpInstancesSecondPage)
                .thenReturn(gcpInstancesThirdPage);

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertEquals(actual.get(INSTANCE_NAME_1), getNetworkForInstance(gcpInstancesFirstPage, INSTANCE_NAME_1));
        assertEquals(actual.get(INSTANCE_NAME_2), getNetworkForInstance(gcpInstancesSecondPage, INSTANCE_NAME_2));
        assertEquals(actual.get(INSTANCE_NAME_3), getNetworkForInstance(gcpInstancesThirdPage, INSTANCE_NAME_3));
    }

    @Test
    public void testProvideShouldReturnsMapWithoutNetworkWhenTheThereAreMissingNodes() throws IOException {
        InstanceList gcpInstances = createGcpInstancesWithMissingNode();
        when(computeInstancesMock.execute()).thenReturn(gcpInstances);

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertEquals(actual.get(INSTANCE_NAME_1), getNetworkForInstance(gcpInstances, INSTANCE_NAME_1));
        assertEquals(actual.get(INSTANCE_NAME_2), getNetworkForInstance(gcpInstances, INSTANCE_NAME_2));
        assertEquals(actual.get(INSTANCE_NAME_3), getNetworkForInstance(gcpInstances, INSTANCE_NAME_3));
    }

    @Test
    public void testProvideShouldReturnsMapWithoutNetworkWhenTheThereAreNoResponseFromGcp() throws IOException {
        when(computeInstancesMock.execute()).thenReturn(new InstanceList());

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertFalse(actual.get(INSTANCE_NAME_1).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_2).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_3).isPresent());
    }

    @Test
    public void testProvideShouldReturnsMapWithoutNetworkWhenTheGcpThrowsException() throws IOException {
        when(computeInstancesMock.execute()).thenThrow(new IOException("Error happened on GCP side."));

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertFalse(actual.get(INSTANCE_NAME_1).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_2).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_3).isPresent());
    }

    private AuthenticatedContext createAuthenticatedContext() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential("1", "gcp-cred", Collections.singletonMap("projectId", "gcp-cred"), false);
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

    private List<CloudResource> createCloudResources() {
        return List.of(
                createCloudResource(INSTANCE_NAME_1),
                createCloudResource(INSTANCE_NAME_2),
                createCloudResource(INSTANCE_NAME_3));
    }

    private CloudResource createCloudResource(String name) {
        return CloudResource.builder()
                .name(name)
                .type(ResourceType.GCP_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(Collections.emptyMap())
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

    private InstanceList createGcpInstancesWithMissingNode() {
        InstanceList instanceList = new InstanceList();
        instanceList.setItems(List.of(
                createInstance(INSTANCE_NAME_1),
                createInstance(INSTANCE_NAME_3)));
        return instanceList;
    }

    private Instance createInstance(String name) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }

    private Optional<NetworkInterface> getNetworkForInstance(InstanceList gcpInstances, String instanceName) {
        return gcpInstances.getItems().stream()
                .filter(instance -> instance.getName().equals(instanceName))
                .findFirst()
                .map(gcpInstance -> gcpInstance.getNetworkInterfaces().get(0));
    }
}
