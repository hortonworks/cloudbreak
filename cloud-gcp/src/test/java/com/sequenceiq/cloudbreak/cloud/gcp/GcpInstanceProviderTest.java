package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
class GcpInstanceProviderTest {

    private static final String PROJECT_ID = "projectId";

    private static final String AVAILABILITY_ZONE = "europe-north1";

    private static final String INSTANCE_NAME = "instanceName";

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
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @BeforeEach
    public void before() {
        when(cloudContext.getName()).thenReturn("stack");
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(compute.instances()).thenReturn(instancesMock);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
    }

    @Test
    public void testProvideShouldReturnsInstances() throws IOException {
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME)).thenReturn(computeInstancesGet);
        Instance instance = createInstance(INSTANCE_NAME);
        when(computeInstancesGet.execute()).thenReturn(instance);

        Optional<Instance> actual = underTest.getInstance(authenticatedContext, INSTANCE_NAME, AVAILABILITY_ZONE);

        assertTrue(actual.isPresent());
        assertEquals(instance, actual.get());
    }

    @Test
    public void testProvideShouldReturnNullWhenThereisNoResponseFromGcp() throws IOException {
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME)).thenReturn(computeInstancesGet);
        when(computeInstancesGet.execute()).thenThrow(new IOException("Error happened on GCP side."));

        Optional<Instance> actual = underTest.getInstance(authenticatedContext, INSTANCE_NAME, AVAILABILITY_ZONE);

        assertFalse(actual.isPresent());
    }

    private Instance createInstance(String name) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }
}