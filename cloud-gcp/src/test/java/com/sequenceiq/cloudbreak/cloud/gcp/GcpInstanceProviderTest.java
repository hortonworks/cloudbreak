package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.common.model.DefaultApplicationTag;

@ExtendWith(MockitoExtension.class)
class GcpInstanceProviderTest {

    private static final String PROJECT_ID = "projectId";

    private static final String AVAILABILITY_ZONE = "europe-north1";

    private static final String INSTANCE_NAME1 = "instanceName";

    private static final String INSTANCE_NAME2 = "instanceName2";

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String MACHINE_TYPE_URL = "https://www.googleapis.com/compute/v1/projects/gcp-dev-cloudbreak/zones/us-west2-a/machineTypes/";

    @InjectMocks
    private GcpInstanceProvider underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Instances instancesMock;

    @Mock
    private Compute.Instances.Get computeInstancesGet;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Captor
    private ArgumentCaptor<String> filterCaptor;

    @BeforeEach
    void before() {
        lenient().when(cloudContext.getName()).thenReturn("stack");
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(compute.instances()).thenReturn(instancesMock);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
    }

    @Test
    void testProvideShouldReturnsInstances() throws IOException {
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME1)).thenReturn(computeInstancesGet);
        Instance instance = createInstance(INSTANCE_NAME1);
        when(computeInstancesGet.execute()).thenReturn(instance);

        Optional<Instance> actual = underTest.getInstance(authenticatedContext, INSTANCE_NAME1, AVAILABILITY_ZONE);

        assertTrue(actual.isPresent());
        assertEquals(instance, actual.get());
    }

    @Test
    void testProvideShouldReturnNullWhenThereisNoResponseFromGcp() throws IOException {
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME1)).thenReturn(computeInstancesGet);
        when(computeInstancesGet.execute()).thenThrow(new IOException("Error happened on GCP side."));

        Optional<Instance> actual = underTest.getInstance(authenticatedContext, INSTANCE_NAME1, AVAILABILITY_ZONE);

        assertFalse(actual.isPresent());
    }

    @Test
    void testCollectCdpInstances() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        Compute.Instances.List computeInstanceList = mock(Compute.Instances.List.class);
        when(cloudInstance.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withInstances(List.of(cloudInstance)).build()));
        when(instancesMock.list(PROJECT_ID, AVAILABILITY_ZONE)).thenReturn(computeInstanceList);
        when(computeInstanceList.setFilter(anyString())).thenReturn(computeInstanceList);
        when(computeInstanceList.execute()).thenReturn(new InstanceList().setItems(List.of(
                createInstance(INSTANCE_NAME1, MACHINE_TYPE_URL + "n2-standard-4", "RUNNING"),
                createInstance(INSTANCE_NAME2, MACHINE_TYPE_URL + "n1-standard-4", "PROVISIONING")
        )));
        when(gcpLabelUtil.transformLabelKeyOrValue(DefaultApplicationTag.RESOURCE_CRN.key())).thenReturn(DefaultApplicationTag.RESOURCE_CRN.key());
        when(gcpLabelUtil.transformLabelKeyOrValue(RESOURCE_CRN)).thenReturn(RESOURCE_CRN);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of());

        verify(computeInstanceList).setFilter(filterCaptor.capture());
        assertEquals("labels." + DefaultApplicationTag.RESOURCE_CRN.key() + " eq " + RESOURCE_CRN, filterCaptor.getValue());
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder(INSTANCE_NAME1, INSTANCE_NAME2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("n2-standard-4", "n1-standard-4");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED, InstanceStatus.IN_PROGRESS);
    }

    @Test
    void testCollectCdpInstancesWithEmptyFirstGroup() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        CloudInstance skeleton = mock(CloudInstance.class);
        Compute.Instances.List computeInstanceList = mock(Compute.Instances.List.class);
        when(cloudInstance.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withSkeleton(skeleton).build(),
                Group.builder().withInstances(List.of(cloudInstance)).build()));
        when(instancesMock.list(PROJECT_ID, AVAILABILITY_ZONE)).thenReturn(computeInstanceList);
        when(computeInstanceList.setFilter(anyString())).thenReturn(computeInstanceList);
        when(computeInstanceList.execute()).thenReturn(new InstanceList().setItems(List.of(
                createInstance(INSTANCE_NAME1, MACHINE_TYPE_URL + "n2-standard-4", "RUNNING"),
                createInstance(INSTANCE_NAME2, MACHINE_TYPE_URL + "n1-standard-4", "PROVISIONING")
        )));
        when(gcpLabelUtil.transformLabelKeyOrValue(DefaultApplicationTag.RESOURCE_CRN.key())).thenReturn(DefaultApplicationTag.RESOURCE_CRN.key());
        when(gcpLabelUtil.transformLabelKeyOrValue(RESOURCE_CRN)).thenReturn(RESOURCE_CRN);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of());

        verify(computeInstanceList).setFilter(filterCaptor.capture());
        assertEquals("labels." + DefaultApplicationTag.RESOURCE_CRN.key() + " eq " + RESOURCE_CRN, filterCaptor.getValue());
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder(INSTANCE_NAME1, INSTANCE_NAME2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("n2-standard-4", "n1-standard-4");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED, InstanceStatus.IN_PROGRESS);
    }

    @Test
    void testCollectCdpInstancesWithEmptyAllGroups() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance skeleton = mock(CloudInstance.class);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withSkeleton(skeleton).build()));

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of());

        assertThat(result).hasSize(0);
    }

    @Test
    void testCollectCdpInstancesWhenThereIsAKnownInstanceIdMissing() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        Compute.Instances.List computeInstanceList = mock(Compute.Instances.List.class);
        when(cloudInstance.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withInstances(List.of(cloudInstance)).build()));
        when(instancesMock.list(PROJECT_ID, AVAILABILITY_ZONE)).thenReturn(computeInstanceList);
        when(computeInstanceList.setFilter(anyString())).thenReturn(computeInstanceList);
        when(computeInstanceList.execute())
                .thenReturn(new InstanceList().setItems(List.of(createInstance(INSTANCE_NAME1, MACHINE_TYPE_URL + "n2-standard-4", "RUNNING"))))
                .thenReturn(new InstanceList().setItems(List.of(createInstance(INSTANCE_NAME2, MACHINE_TYPE_URL + "n1-standard-4", "PROVISIONING"))));
        when(gcpLabelUtil.transformLabelKeyOrValue(DefaultApplicationTag.RESOURCE_CRN.key())).thenReturn(DefaultApplicationTag.RESOURCE_CRN.key());
        when(gcpLabelUtil.transformLabelKeyOrValue(RESOURCE_CRN)).thenReturn(RESOURCE_CRN);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of(INSTANCE_NAME2));

        verify(computeInstanceList, times(2)).setFilter(filterCaptor.capture());
        assertEquals("labels." + DefaultApplicationTag.RESOURCE_CRN.key() + " eq " + RESOURCE_CRN, filterCaptor.getAllValues().get(0));
        assertEquals("name eq \"(" + INSTANCE_NAME2 + ")\"", filterCaptor.getAllValues().get(1));
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder(INSTANCE_NAME1, INSTANCE_NAME2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("n2-standard-4", "n1-standard-4");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED, InstanceStatus.IN_PROGRESS);
    }

    @Test
    void testCollectCdpInstancesWhenThrows() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withInstances(List.of(cloudInstance)).build()));
        when(instancesMock.list(PROJECT_ID, AVAILABILITY_ZONE)).thenThrow(new IOException("Error happened on GCP side."));

        List<InstanceCheckMetadata> result = assertDoesNotThrow(() -> underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of()));

        assertThat(result).isEmpty();
    }

    @Test
    void testCollectCdpInstancesWhenInstanceByNameListIsNull() throws IOException {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        Compute.Instances.List computeInstanceList = mock(Compute.Instances.List.class);
        when(cloudInstance.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(cloudStack.getGroups()).thenReturn(List.of(Group.builder().withInstances(List.of(cloudInstance)).build()));
        when(instancesMock.list(PROJECT_ID, AVAILABILITY_ZONE)).thenReturn(computeInstanceList);
        when(computeInstanceList.setFilter(anyString())).thenReturn(computeInstanceList);
        when(computeInstanceList.execute())
                .thenReturn(new InstanceList().setItems(List.of(createInstance(INSTANCE_NAME1, MACHINE_TYPE_URL + "n2-standard-4", "RUNNING"))))
                .thenReturn(new InstanceList());
        when(gcpLabelUtil.transformLabelKeyOrValue(DefaultApplicationTag.RESOURCE_CRN.key())).thenReturn(DefaultApplicationTag.RESOURCE_CRN.key());
        when(gcpLabelUtil.transformLabelKeyOrValue(RESOURCE_CRN)).thenReturn(RESOURCE_CRN);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(authenticatedContext, RESOURCE_CRN, cloudStack, List.of(INSTANCE_NAME2));

        verify(computeInstanceList, times(2)).setFilter(filterCaptor.capture());
        assertEquals("labels." + DefaultApplicationTag.RESOURCE_CRN.key() + " eq " + RESOURCE_CRN, filterCaptor.getAllValues().get(0));
        assertEquals("name eq \"(" + INSTANCE_NAME2 + ")\"", filterCaptor.getAllValues().get(1));
        assertThat(result).hasSize(1);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder(INSTANCE_NAME1);
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("n2-standard-4");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED);
    }

    private Instance createInstance(String name) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }

    private Instance createInstance(String name, String machineType, String status) {
        Instance instance = createInstance(name);
        instance.setMachineType(machineType);
        instance.setStatus(status);
        return instance;
    }
}