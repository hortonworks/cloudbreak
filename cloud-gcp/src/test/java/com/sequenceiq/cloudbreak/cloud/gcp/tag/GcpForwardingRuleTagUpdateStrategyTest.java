package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.RegionSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpForwardingRuleTagUpdateStrategyTest {
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
    private Compute.ForwardingRules forwardingRules;

    @Mock
    private Compute.ForwardingRules.Get forwardingRulesGet;

    @Mock
    private Compute.ForwardingRules.SetLabels forwardingRulesSetLabels;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @InjectMocks
    private GcpForwardingRuleTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
    }

    @Test
    void testUpdateTags() throws Exception {
        CloudResource cloudResource = buildCloudResource(ResourceType.GCP_FORWARDING_RULE);
        ForwardingRule forwardingRule = new ForwardingRule()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(EXISTING_LABELS);

        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.get(PROJECT_ID, REGION, RESOURCE_NAME)).thenReturn(forwardingRulesGet);
        when(forwardingRulesGet.execute()).thenReturn(forwardingRule);
        when(forwardingRules.setLabels(eq(PROJECT_ID), eq(REGION), eq(RESOURCE_NAME), any(RegionSetLabelsRequest.class)))
                .thenReturn(forwardingRulesSetLabels);

        underTest.updateTags(authenticatedContext, cloudResource, NEW_LABELS);

        verify(forwardingRules).setLabels(eq(PROJECT_ID), eq(REGION), eq(RESOURCE_NAME),
                argThat(req -> FINGERPRINT.equals(req.getLabelFingerprint())
                        && MERGED_LABELS.equals(req.getLabels())));
        verify(forwardingRulesSetLabels).execute();
    }

    private CloudResource buildCloudResource(ResourceType type) {
        return CloudResource.builder()
                .withType(type)
                .withName(RESOURCE_NAME)
                .withAvailabilityZone(ZONE)
                .build();
    }
}