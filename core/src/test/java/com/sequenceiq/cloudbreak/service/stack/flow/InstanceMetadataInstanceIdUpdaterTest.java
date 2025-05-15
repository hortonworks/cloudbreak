package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class InstanceMetadataInstanceIdUpdaterTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudConnector cloudConnector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StackCreationContext stackCreationContext;

    @InjectMocks
    private InstanceMetadataInstanceIdUpdater underTest;

    @BeforeEach
    void setUp() {
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        when(stackCreationContext.getCloudContext().getPlatformVariant()).thenReturn(new CloudPlatformVariant("MOCK", "MOCK"));
    }

    @Test
    void testUpdateWithInstanceIdAndStatusShouldThrowExceptionWhenTransactionFails() throws TransactionService.TransactionExecutionException {
        List<CloudResourceStatus> affectedResources = List.of();
        doThrow(new TransactionService.TransactionExecutionException("the transaction failed", new RuntimeException()))
                .when(transactionService).required(any(Runnable.class));

        Assertions.assertThrows(TransactionService.TransactionRuntimeExecutionException.class,
                () -> underTest.updateWithInstanceIdAndStatus(stackCreationContext, affectedResources));
    }

    @Test
    void testUpdateWithInstanceIdAndStatusWhenNoInstanceMetadataFoundInRequestedState() throws TransactionService.TransactionExecutionException {
        List<CloudResourceStatus> affectedResources = List.of();
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getInstanceResourceType()).thenReturn(ResourceType.MOCK_INSTANCE);
        when(instanceMetaDataService.findAllByStackIdAndStatus(anyLong(), eq(InstanceStatus.REQUESTED))).thenReturn(List.of());
        when(authenticatedContext.getCloudContext().getId()).thenReturn(1L);

        underTest.updateWithInstanceIdAndStatus(stackCreationContext, affectedResources);

        verify(resourceConnector, times(1)).getInstanceResourceType();
        ArgumentCaptor<List<InstanceMetaData>> instanceMetadataListCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataListCaptor.capture());
        List<InstanceMetaData> instanceMetadataList = instanceMetadataListCaptor.getValue();
        Assertions.assertTrue(instanceMetadataList.isEmpty());
    }

    @Test
    void testUpdateWithInstanceIdAndStatusShouldPersistInstanceIdAndStatus() throws TransactionService.TransactionExecutionException {
        String groupName = "group1";
        List<String> resourceInstanceIds = List.of("i-1", "i-2");
        List<CloudResourceStatus> affectedResources = List.of(
                new CloudResourceStatus(getCloudInstanceResource(groupName, 1L, resourceInstanceIds.get(0)), ResourceStatus.CREATED),
                new CloudResourceStatus(getCloudInstanceResource(groupName, 2L, resourceInstanceIds.get(1)), ResourceStatus.CREATED)
        );
        doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.getInstanceResourceType()).thenReturn(ResourceType.MOCK_INSTANCE);
        when(authenticatedContext.getCloudContext().getId()).thenReturn(1L);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName);
        when(instanceMetaDataService.findAllByStackIdAndStatus(anyLong(), eq(InstanceStatus.REQUESTED)))
                .thenReturn(List.of(getInstanceMetaData(instanceGroup, 1L), getInstanceMetaData(instanceGroup, 2L)));

        underTest.updateWithInstanceIdAndStatus(stackCreationContext, affectedResources);

        verify(resourceConnector, times(1)).getInstanceResourceType();
        ArgumentCaptor<List<InstanceMetaData>> instanceMetadataListCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataListCaptor.capture());
        List<InstanceMetaData> instanceMetadataList = instanceMetadataListCaptor.getValue();
        Assertions.assertFalse(instanceMetadataList.isEmpty());
        Assertions.assertTrue(instanceMetadataList.stream()
                .allMatch(im -> resourceInstanceIds.contains(im.getInstanceId()) && im.getInstanceStatus() == InstanceStatus.CREATED));
    }

    private CloudResource getCloudInstanceResource(String groupName, long privateId, String instanceId) {
        return CloudResource.builder()
                .withGroup(groupName)
                .withPrivateId(privateId)
                .withType(ResourceType.MOCK_INSTANCE)
                .withInstanceId(instanceId)
                .withName(instanceId)
                .withParameters(Map.of())
                .build();
    }

    private InstanceMetaData getInstanceMetaData(InstanceGroup instanceGroup, Long privateId) {
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setPrivateId(privateId);
        instanceMetaData1.setInstanceGroup(instanceGroup);
        return instanceMetaData1;
    }
}