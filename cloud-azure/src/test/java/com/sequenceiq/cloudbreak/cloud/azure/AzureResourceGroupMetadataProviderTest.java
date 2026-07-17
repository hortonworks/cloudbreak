package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@ExtendWith(MockitoExtension.class)
class AzureResourceGroupMetadataProviderTest {

    private static final int MAX_RESOURCE_NAME_LENGTH = 50;

    private static final String EXPLICIT_RESOURCE_GROUP = "explicitResourceGroup";

    @Mock
    private CloudStack cloudStack;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private DynamicModel dynamicModel;

    @InjectMocks
    private AzureResourceGroupMetadataProvider underTest;

    private CloudContext cloudContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", MAX_RESOURCE_NAME_LENGTH);
        cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("mystack")
                .withCrn("crn")
                .withPlatform("AZURE")
                .build();
    }

    @Test
    void getResourceGroupNameForCloudStackReturnsExplicitParameter() {
        when(cloudStack.getParameters()).thenReturn(Map.of(RESOURCE_GROUP_NAME_PARAMETER, EXPLICIT_RESOURCE_GROUP));

        String result = underTest.getResourceGroupName(cloudContext, cloudStack);

        assertEquals(EXPLICIT_RESOURCE_GROUP, result);
    }

    @Test
    void getResourceGroupNameForCloudStackFallsBackToStackName() {
        when(cloudStack.getParameters()).thenReturn(Map.of());

        String result = underTest.getResourceGroupName(cloudContext, cloudStack);

        assertEquals("mystack1", result);
    }

    @Test
    void getResourceGroupNameForDatabaseStackReturnsExplicitParameter() {
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getParameters()).thenReturn(Map.of(RESOURCE_GROUP_NAME_PARAMETER, EXPLICIT_RESOURCE_GROUP));

        String result = underTest.getResourceGroupName(cloudContext, databaseStack);

        assertEquals(EXPLICIT_RESOURCE_GROUP, result);
    }

    @Test
    void getResourceGroupNameForDynamicModelReturnsExplicitParameter() {
        when(dynamicModel.getParameters()).thenReturn(Map.of(RESOURCE_GROUP_NAME_PARAMETER, EXPLICIT_RESOURCE_GROUP));

        String result = underTest.getResourceGroupName(cloudContext, dynamicModel);

        assertEquals(EXPLICIT_RESOURCE_GROUP, result);
    }

    @Test
    void getResourceGroupNameForCloudStackReturnsExplicitParameterWithNullCloudContext() {
        when(cloudStack.getParameters()).thenReturn(Map.of(RESOURCE_GROUP_NAME_PARAMETER, EXPLICIT_RESOURCE_GROUP));

        String result = underTest.getResourceGroupName(null, cloudStack);

        assertEquals(EXPLICIT_RESOURCE_GROUP, result);
    }

    @Test
    void getResourceGroupNameForCloudStackReturnsNullWhenNoExplicitParameterAndNullCloudContext() {
        when(cloudStack.getParameters()).thenReturn(Map.of());

        String result = underTest.getResourceGroupName(null, cloudStack);

        assertNull(result);
    }

    @Test
    void getResourceGroupNameForDatabaseStackReturnsExplicitParameterWithNullCloudContext() {
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getParameters()).thenReturn(Map.of(RESOURCE_GROUP_NAME_PARAMETER, EXPLICIT_RESOURCE_GROUP));

        String result = underTest.getResourceGroupName(null, databaseStack);

        assertEquals(EXPLICIT_RESOURCE_GROUP, result);
    }

    @Test
    void getResourceGroupNameForDynamicModelReturnsNullWhenNoExplicitParameterAndNullCloudContext() {
        when(dynamicModel.getParameters()).thenReturn(Map.of());

        String result = underTest.getResourceGroupName(null, dynamicModel);

        assertNull(result);
    }
}
