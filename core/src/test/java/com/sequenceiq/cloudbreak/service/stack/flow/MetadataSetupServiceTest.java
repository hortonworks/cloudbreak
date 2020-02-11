package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@RunWith(MockitoJUnitRunner.class)
public class MetadataSetupServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final Long PRIVATE_ID = 2L;

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String PRIVATE_IP = "PRIVATE_IP";

    private static final String PUBLIC_IP = "PUBLIC_IP";

    private static final int SSH_PORT = 22;

    private static final String LOCALITY_INDICATOR = "LOCALITY_INDICATOR";

    private static final Long INSTANCE_GROUP_ID = 3L;

    private static final long CURRENT_TIME = System.currentTimeMillis();

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus CREATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus TERMINATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus SERVICES_RUNNING =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ImageService imageService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Clock clock;

    @InjectMocks
    private MetadataSetupService underTest;

    @Captor
    private ArgumentCaptor<InstanceMetaData> captor;

    @Test
    public void shouldNotSaveInstancesWhenImageNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED);
        doThrow(CloudbreakImageNotFoundException.class).when(imageService).getImage(STACK_ID);

        expectedException.expectMessage("Instance metadata collection failed");
        expectedException.expect(CloudbreakServiceException.class);

        underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);
    }

    @Test
    public void testOneNewInstance() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(anyLong())).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED);

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);

        assertEquals(1, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceGroupService).findByStackId(STACK_ID);
        verify(instanceMetaDataService).save(captor.capture());
        InstanceMetaData instanceMetaData = captor.getValue();
        assertCommonProperties(instanceMetaData);
        assertEquals(CREATED, instanceMetaData.getInstanceStatus());
        assertNotNull(instanceMetaData.getImage());
    }

    @Test
    public void testOneTerminatedInstance() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(anyLong())).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.TERMINATED);

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(0, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceGroupService).findByStackId(STACK_ID);
        verify(instanceMetaDataService).save(captor.capture());
        InstanceMetaData instanceMetaData = captor.getValue();
        assertCommonProperties(instanceMetaData);
        assertEquals(TERMINATED, instanceMetaData.getInstanceStatus());
        assertNull(instanceMetaData.getImage());
    }

    private Image getEmptyImage() {
        return new Image(null, null, null, null, null, null, null, null);
    }

    private Iterable<CloudVmMetaDataStatus> getCloudVmMetaDataStatuses(InstanceStatus instanceStatus) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(null, GROUP_NAME, PRIVATE_ID, List.of(), null, Map.of(), null, null);
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, SUBNET_ID);
        params.put(CloudInstance.INSTANCE_NAME, INSTANCE_NAME);
        CloudInstance cloudInstance = new CloudInstance(null, instanceTemplate, null, params);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, instanceStatus);
        CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(PRIVATE_IP, PUBLIC_IP, SSH_PORT, LOCALITY_INDICATOR);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
        return List.of(cloudVmMetaDataStatus);
    }

    private void assertCommonProperties(InstanceMetaData instanceMetaData) {
        assertEquals(PRIVATE_IP, instanceMetaData.getPrivateIp());
        assertEquals(PUBLIC_IP, instanceMetaData.getPublicIp());
        assertEquals(SSH_PORT, instanceMetaData.getSshPort());
        assertEquals(LOCALITY_INDICATOR, instanceMetaData.getLocalityIndicator());
        assertEquals(INSTANCE_GROUP_ID, instanceMetaData.getInstanceGroup().getId());
        assertNull(instanceMetaData.getInstanceId());
        assertEquals(PRIVATE_ID, instanceMetaData.getPrivateId());
        assertEquals(CURRENT_TIME, instanceMetaData.getStartDate());
        assertEquals(SUBNET_ID, instanceMetaData.getSubnetId());
        assertEquals(INSTANCE_NAME, instanceMetaData.getInstanceName());
        assertEquals(Boolean.FALSE, instanceMetaData.getAmbariServer());
        assertEquals(Boolean.FALSE, instanceMetaData.getClusterManagerServer());
        assertEquals(InstanceMetadataType.CORE, instanceMetaData.getInstanceMetadataType());
    }

}