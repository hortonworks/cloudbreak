package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.context.VolumeMatcher;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureContextServiceTest {

    private static final long PRIVATE_ID_1 = 1L;

    private static final long PRIVATE_ID_2 = 2L;

    @Mock
    private VolumeMatcher volumeMatcher;

    @InjectMocks
    private AzureContextService underTest;

    @Test
    void testAddInstancesToContext() {
        ResourceBuilderContext context = mock(ResourceBuilderContext.class);
        underTest.addInstancesToContext(List.of(instance(PRIVATE_ID_1), instance(PRIVATE_ID_2)), context, List.of(group()));
        ArgumentCaptor<Collection<CloudResource>> cloudResourcesCaptor1 = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<CloudResource>> cloudResourcesCaptor2 = ArgumentCaptor.forClass(Collection.class);
        verify(context, times(1)).addComputeResources(eq(PRIVATE_ID_1), cloudResourcesCaptor1.capture());
        verify(context, times(1)).addComputeResources(eq(PRIVATE_ID_2), cloudResourcesCaptor2.capture());
        assertAddedCloudResources(cloudResourcesCaptor1, PRIVATE_ID_1);
        assertAddedCloudResources(cloudResourcesCaptor2, PRIVATE_ID_2);
    }

    private static void assertAddedCloudResources(ArgumentCaptor<Collection<CloudResource>> cloudResourcesCaptor, long privateId) {
        Collection<CloudResource> cloudResources = cloudResourcesCaptor.getValue();
        assertThat(cloudResources).hasSize(1);
        CloudResource cloudResource = cloudResources.iterator().next();
        assertTrue(cloudResource.getParameters().containsKey(CloudResource.PRIVATE_ID));
        assertEquals(privateId, cloudResource.getParameters().get(CloudResource.PRIVATE_ID));
    }

    @NotNull
    private static Group group() {
        return new Group("worker", InstanceGroupType.CORE, List.of(cloudInstance(PRIVATE_ID_1), cloudInstance(PRIVATE_ID_2)), null, null,
                null, null, null, 0, Optional.empty(), null, emptyMap(), null);
    }

    @NotNull
    private static CloudInstance cloudInstance(long privateId) {
        return new CloudInstance(null, instanceTemplate(privateId), null, null, null);
    }

    @NotNull
    private static InstanceTemplate instanceTemplate(long privateId) {
        return new InstanceTemplate("flavor", "worker", privateId, new ArrayList<>(), InstanceStatus.CREATE_REQUESTED, new HashMap<>(),
                10L, "imageId", null, null);
    }

    private static CloudResource instance(long privateId) {
        return CloudResource.builder()
                .withType(ResourceType.AZURE_INSTANCE)
                .withName("name" + privateId)
                .withGroup("worker")
                .withParameters(Map.of(CloudResource.PRIVATE_ID, privateId))
                .build();
    }

}