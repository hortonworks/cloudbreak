package com.sequenceiq.cloudbreak.cloud.azure.logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@ExtendWith(MockitoExtension.class)
class AzureResourceGroupLoggingAspectTest {

    private static final String RESOURCE_GROUP = "myResourceGroup";

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private AzureResourceGroupLoggingAspect underTest;

    @BeforeEach
    void setUp() {
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
    }

    @Test
    void resolveResourceGroupNameFromCloudStack() {
        CloudStack cloudStack = mock(CloudStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn(RESOURCE_GROUP);

        String result = underTest.resolveResourceGroupName(new Object[]{authenticatedContext, cloudStack});

        assertEquals(RESOURCE_GROUP, result);
    }

    @Test
    void resolveResourceGroupNameFromDatabaseStack() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP);

        String result = underTest.resolveResourceGroupName(new Object[]{authenticatedContext, databaseStack});

        assertEquals(RESOURCE_GROUP, result);
    }

    @Test
    void resolveResourceGroupNameFromCloudInstanceInList() {
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(eq(cloudContext), any(DynamicModel.class))).thenReturn(RESOURCE_GROUP);

        String result = underTest.resolveResourceGroupName(new Object[]{authenticatedContext, List.of(cloudInstance)});

        assertEquals(RESOURCE_GROUP, result);
    }

    @Test
    void resolveResourceGroupNameFromCloudStackWithoutAuthenticatedContext() {
        CloudStack cloudStack = mock(CloudStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(null, cloudStack)).thenReturn(RESOURCE_GROUP);

        String result = underTest.resolveResourceGroupName(new Object[]{cloudStack});

        assertEquals(RESOURCE_GROUP, result);
    }

    @Test
    void resolveResourceGroupNameReturnsUnknownWhenProviderReturnsBlank() {
        CloudStack cloudStack = mock(CloudStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(null, cloudStack)).thenReturn(null);

        String result = underTest.resolveResourceGroupName(new Object[]{cloudStack});

        assertEquals("unknown", result);
    }

    @Test
    void resolveResourceGroupNameReturnsUnknownWhenNoResourceGroupSourcePresent() {
        String result = underTest.resolveResourceGroupName(new Object[]{authenticatedContext});

        assertEquals("unknown", result);
    }

    @Test
    void logResourceGroupSwallowsExceptions() {
        CloudStack cloudStack = mock(CloudStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenThrow(new RuntimeException("boom"));
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{authenticatedContext, cloudStack});

        assertDoesNotThrow(() -> underTest.logResourceGroup(joinPoint));
    }
}
