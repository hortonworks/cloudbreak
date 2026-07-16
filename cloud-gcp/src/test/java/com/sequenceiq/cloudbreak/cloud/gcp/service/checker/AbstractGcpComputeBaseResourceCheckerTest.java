package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpOperationUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AbstractGcpComputeBaseResourceCheckerTest {

    @Mock
    private GcpComputeResourceChecker resourceChecker;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpResourceNameService resourceNameService;

    @InjectMocks
    private ConcreteChecker underTest;

    private GcpContext buildContext;

    private GcpContext deleteContext;

    private AuthenticatedContext auth;

    @BeforeEach
    void setUp() {
        buildContext = mock(GcpContext.class);
        lenient().when(buildContext.isBuild()).thenReturn(true);
        lenient().when(buildContext.getName()).thenReturn("test-stack");

        deleteContext = mock(GcpContext.class);
        lenient().when(deleteContext.isBuild()).thenReturn(false);
        lenient().when(deleteContext.getName()).thenReturn("test-stack");

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withName("test-stack")
                .withId(1L)
                .build();
        auth = new AuthenticatedContext(cloudContext, null);
    }

    @Test
    void checkResourcesWhenOperationFinishedAndBuildContextReturnsCreated() throws Exception {
        Operation operation = new Operation();
        when(resourceChecker.check(any(), any(), any())).thenReturn(operation);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(true);

        List<CloudResourceStatus> result = underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(minimalResource()));

        assertEquals(1, result.size());
        assertEquals(ResourceStatus.CREATED, result.get(0).getStatus());
    }

    @Test
    void checkResourcesWhenOperationFinishedAndDeleteContextReturnsDeleted() throws Exception {
        Operation operation = new Operation();
        when(resourceChecker.check(any(), any(), any())).thenReturn(operation);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(true);

        List<CloudResourceStatus> result = underTest.checkResources(ResourceType.GCP_INSTANCE, deleteContext, auth, List.of(minimalResource()));

        assertEquals(1, result.size());
        assertEquals(ResourceStatus.DELETED, result.get(0).getStatus());
    }

    @Test
    void checkResourcesWhenOperationInProgressReturnsInProgress() throws Exception {
        Operation operation = new Operation();
        when(resourceChecker.check(any(), any(), any())).thenReturn(operation);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(false);

        List<CloudResourceStatus> result = underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(minimalResource()));

        assertEquals(1, result.size());
        assertEquals(ResourceStatus.IN_PROGRESS, result.get(0).getStatus());
    }

    @Test
    void checkResourcesWhenOperationIsNullTreatsAsFinished() throws Exception {
        when(resourceChecker.check(any(), any(), any())).thenReturn(null);

        List<CloudResourceStatus> result = underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(minimalResource()));

        assertEquals(1, result.size());
        assertEquals(ResourceStatus.CREATED, result.get(0).getStatus());
    }

    @Test
    void checkResourcesWhenExceptionThrownWrapsInGcpResourceException() throws Exception {
        when(resourceChecker.check(any(), any(), any())).thenThrow(new RuntimeException("check failed"));

        assertThrows(GcpResourceException.class,
                () -> underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(minimalResource())));
    }

    @Test
    void checkResourcesUsesOperationInfoWhenPresent() throws Exception {
        OperationInfo operationInfo = new OperationInfo(OperationType.ZONAL, "op-456");
        Map<String, Object> params = new HashMap<>();
        params.put(GcpOperationUtil.OPERATION_INFO, operationInfo);
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withName("instance-1")
                .withStatus(CommonStatus.CREATED)
                .withParameters(params)
                .build();
        Operation operation = new Operation();
        when(resourceChecker.check(any(), any(), any())).thenReturn(operation);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(true);

        underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(resource));

        ArgumentCaptor<OperationInfo> captor = ArgumentCaptor.forClass(OperationInfo.class);
        verify(resourceChecker).check(any(), captor.capture(), any());
        assertEquals(OperationType.ZONAL, captor.getValue().operationType());
        assertEquals("op-456", captor.getValue().operationId());
    }

    @Test
    void checkResourcesUsesLegacyOperationIdWhenOperationInfoAbsent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(GcpOperationUtil.OPERATION_ID, "legacy-op-789");
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withName("instance-1")
                .withStatus(CommonStatus.CREATED)
                .withParameters(params)
                .build();
        Operation operation = new Operation();
        when(resourceChecker.check(any(), any(), any())).thenReturn(operation);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(true);

        underTest.checkResources(ResourceType.GCP_INSTANCE, buildContext, auth, List.of(resource));

        ArgumentCaptor<OperationInfo> captor = ArgumentCaptor.forClass(OperationInfo.class);
        verify(resourceChecker).check(any(), captor.capture(), any());
        assertEquals(OperationType.UNKNOWN, captor.getValue().operationType());
        assertEquals("legacy-op-789", captor.getValue().operationId());
    }

    @Test
    void createOperationAwareCloudResourceStoresOperationInfo() {
        Operation operation = new Operation();
        operation.setName("op-name");
        operation.setZone("zone");

        CloudResource result = underTest.createOperationAwareCloudResource(minimalResource(), operation);

        assertFalse(result.isPersistent());
        assertEquals(ResourceType.GCP_INSTANCE, result.getType());
        OperationInfo stored = result.getParameter(GcpOperationUtil.OPERATION_INFO, OperationInfo.class);
        assertNotNull(stored);
        assertEquals(OperationType.ZONAL, stored.operationType());
        assertEquals("op-name", stored.operationId());
    }

    @Test
    void createOperationAwareCloudResourceWhenNullOperationStoresNoOperationInfo() {
        CloudResource result = underTest.createOperationAwareCloudResource(minimalResource(), null);

        assertFalse(result.isPersistent());
        assertNull(result.getParameter(GcpOperationUtil.OPERATION_INFO, OperationInfo.class));
    }

    @Test
    void createOperationAwareCloudInstanceStoresOperationIdInParams() {
        CloudInstance instance = mock(CloudInstance.class);
        when(instance.getInstanceId()).thenReturn("inst-id");
        when(instance.getSubnetId()).thenReturn("subnet-1");
        when(instance.getAvailabilityZone()).thenReturn("us-east1-b");

        Operation operation = new Operation();
        operation.setName("op-name");

        CloudInstance result = underTest.createOperationAwareCloudInstance(instance, operation);

        assertEquals("op-name", result.getParameters().get(GcpOperationUtil.OPERATION_ID));
    }

    private CloudResource minimalResource() {
        return CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withName("instance-1")
                .withStatus(CommonStatus.CREATED)
                .withParameters(new HashMap<>())
                .build();
    }

    static class ConcreteChecker extends AbstractGcpComputeBaseResourceChecker {
    }
}
