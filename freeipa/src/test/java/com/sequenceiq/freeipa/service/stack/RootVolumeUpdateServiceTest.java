package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DiskUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpdateRootVolumeResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.RootVolumeUpdateEvent;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@ExtendWith(MockitoExtension.class)
class RootVolumeUpdateServiceTest {

    private static final String ENVIRONMENT_ID1 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ENVIRONMENT_ID2 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:98765-4321";

    private static final String ACCOUNT_ID = "accountId";

    private static Stack stack1;

    private static Stack stack2;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private RootVolumeUpdateService underTest;

    @BeforeEach
    public void setUp() {
        stack1 = new Stack();
        stack1.setResourceCrn(ENVIRONMENT_ID1);
        stack1.setEnvironmentCrn(ENVIRONMENT_ID1);
        stack1.setCloudPlatform("AWS");
        stack1.setPlatformvariant("AWS");
        InstanceGroup instanceGroup = new InstanceGroup();
        stack1.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host.domain");
        instanceMetaData.setInstanceId("instance_1");

        stack2 = new Stack();
        stack2.setResourceCrn(ENVIRONMENT_ID2);
        stack2.setEnvironmentCrn(ENVIRONMENT_ID2);
        stack2.setCloudPlatform("AWS");
        stack2.setPlatformvariant("AWS");
        Template template = new Template();
        template.setRootVolumeSize(100);
        template.setRootVolumeType("gp3");
        instanceGroup = new InstanceGroup();
        stack2.getInstanceGroups().add(instanceGroup);
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setTemplate(template);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData2));
        instanceMetaData2.setDiscoveryFQDN("host1.domain");
        instanceMetaData2.setInstanceId("instance_1");
        instanceMetaData2 = new InstanceMetaData();
        instanceGroup.getInstanceMetaData().add(instanceMetaData2);
        instanceMetaData2.setDiscoveryFQDN("host2.domain");
        instanceMetaData2.setInstanceId("instance_2");
    }

    @Test
    void testRootVolumeDiskUpdate() throws Exception {
        mockFields();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID2, ACCOUNT_ID)).thenReturn(stack2);
        when(defaultRootVolumeSizeProvider.getForPlatform(anyString())).thenReturn(100);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(120);
        diskUpdateRequest.setVolumeType("gp2");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.MODIFY_ROOT_VOLUME, Set.of(ENVIRONMENT_ID2),
                Collections.emptySet())).thenReturn(createOperation());
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "1");
        when(flowManager.notify(anyString(), any(RootVolumeUpdateEvent.class))).thenReturn(flowIdentifier);
        UpdateRootVolumeResponse response = underTest.updateRootVolume(ENVIRONMENT_ID2, diskUpdateRequest, ACCOUNT_ID);

        assertEquals(FlowType.FLOW_CHAIN, response.getFlowIdentifier().getType());
        assertEquals("1", response.getFlowIdentifier().getPollableId());
    }

    @Test
    void testRootVolumeDiskUpdateFailedOperation() {
        mockFields();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID2, ACCOUNT_ID)).thenReturn(stack2);
        when(defaultRootVolumeSizeProvider.getForPlatform(anyString())).thenReturn(100);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(120);
        diskUpdateRequest.setVolumeType("gp2");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.MODIFY_ROOT_VOLUME, Set.of(ENVIRONMENT_ID2),
                Collections.emptySet())).thenReturn(createOperation());
        when(flowManager.notify(anyString(), any(RootVolumeUpdateEvent.class))).thenThrow(new RuntimeException("test"));
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateRootVolume(ENVIRONMENT_ID2, diskUpdateRequest, ACCOUNT_ID));

        assertEquals(exception.getMessage(), "Couldn't start Freeipa Root Volume Update flow: test");
    }

    @Test
    void testRootVolumeDiskUpdateNoUpdateRequired() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID2, ACCOUNT_ID)).thenReturn(stack2);
        when(defaultRootVolumeSizeProvider.getForPlatform(anyString())).thenReturn(100);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp3");
        BadRequestException exception =  assertThrows(BadRequestException.class,
                () -> underTest.updateRootVolume(ENVIRONMENT_ID2, diskUpdateRequest, ACCOUNT_ID));

        assertEquals(exception.getMessage(), "No update required.");
    }

    @Test
    void testRootVolumeDiskUpdatePlatformNotSupported() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID2, ACCOUNT_ID)).thenReturn(stack2);
        stack2.setCloudPlatform("GCP");
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp3");
        BadRequestException exception =  assertThrows(BadRequestException.class,
                () -> underTest.updateRootVolume(ENVIRONMENT_ID2, diskUpdateRequest, ACCOUNT_ID));

        assertEquals(exception.getMessage(), "Root Volume Update for type 'gp3'is not supported for cloud platform: GCP");
    }

    private void mockFields() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("host.domain");
        instanceMetaData.setInstanceId("instance_1");

        when(instanceMetaDataService.getPrimaryGwInstance(any())).thenReturn(instanceMetaData);
    }

    private Operation createOperation() {
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setStatus(OperationState.RUNNING);
        return operation;
    }
}
