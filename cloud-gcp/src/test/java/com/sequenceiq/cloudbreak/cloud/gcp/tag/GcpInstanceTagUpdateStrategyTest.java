package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstancesSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpInstanceTagUpdateStrategyTest {
    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String REGION = "us-central1";

    private static final String RESOURCE_NAME = "test-resource";

    private static final String FINGERPRINT = "abc123fingerprint";

    private static final Map<String, String> EXISTING_LABELS = Map.of("existingKey", "existingValue");

    private static final Map<String, String> NEW_LABELS = Map.of("newKey", "newValue");

    private static final Map<String, String> MERGED_LABELS = Map.of("existingKey", "existingValue", "newKey", "newValue");

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Instances instances;

    @Mock
    private Compute.Instances.Get instancesGet;

    @Mock
    private Compute.Instances.SetLabels instancesSetLabels;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @InjectMocks
    private GcpInstanceTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
    }

    @Test
    void testUpdateTags() throws Exception {
        CloudResource cloudResource = buildCloudResource(ResourceType.GCP_INSTANCE);
        Instance instance = new Instance()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(EXISTING_LABELS);

        when(compute.instances()).thenReturn(instances);
        when(instances.get(PROJECT_ID, ZONE, RESOURCE_NAME)).thenReturn(instancesGet);
        when(instancesGet.execute()).thenReturn(instance);
        when(instances.setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME), any(InstancesSetLabelsRequest.class)))
                .thenReturn(instancesSetLabels);

        underTest.updateTags(authenticatedContext, cloudResource, NEW_LABELS);

        verify(instances).setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME),
                argThat(req -> FINGERPRINT.equals(req.getLabelFingerprint())
                        && MERGED_LABELS.equals(req.getLabels())));
        verify(instancesSetLabels).execute();
    }

    @Test
    void testUpdateTagsWithoutNewTags() throws Exception {
        CloudResource cloudResource = buildCloudResource(ResourceType.GCP_INSTANCE);
        Instance instance = new Instance()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(EXISTING_LABELS);

        when(compute.instances()).thenReturn(instances);
        when(instances.get(PROJECT_ID, ZONE, RESOURCE_NAME)).thenReturn(instancesGet);
        when(instancesGet.execute()).thenReturn(instance);

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_LABELS);

        verify(instancesSetLabels, times(0)).execute();
    }

    @Test
    void testUpdateTagsWithNullExistingTags() throws Exception {
        CloudResource cloudResource = buildCloudResource(ResourceType.GCP_INSTANCE);
        Instance instance = new Instance()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(null);

        when(compute.instances()).thenReturn(instances);
        when(instances.get(PROJECT_ID, ZONE, RESOURCE_NAME)).thenReturn(instancesGet);
        when(instancesGet.execute()).thenReturn(instance);
        when(instances.setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME), any(InstancesSetLabelsRequest.class)))
                .thenReturn(instancesSetLabels);

        underTest.updateTags(authenticatedContext, cloudResource, NEW_LABELS);

        verify(instances).setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME),
                argThat(req -> FINGERPRINT.equals(req.getLabelFingerprint())
                        && NEW_LABELS.equals(req.getLabels())));
        verify(instancesSetLabels).execute();
    }

    private CloudResource buildCloudResource(ResourceType type) {
        return CloudResource.builder()
                .withType(type)
                .withName(RESOURCE_NAME)
                .withAvailabilityZone(ZONE)
                .build();
    }
}