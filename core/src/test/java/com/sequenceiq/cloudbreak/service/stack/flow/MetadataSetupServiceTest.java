package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;
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

@ExtendWith(MockitoExtension.class)
public class MetadataSetupServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final Long PRIVATE_ID = 2L;

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final String PRIVATE_IP = "PRIVATE_IP";

    private static final String PUBLIC_IP = "PUBLIC_IP";

    private static final Integer SSH_PORT = 22;

    private static final String LOCALITY_INDICATOR = "LOCALITY_INDICATOR";

    private static final Long INSTANCE_GROUP_ID = 3L;

    private static final Long CURRENT_TIME = System.currentTimeMillis();

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus CREATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus TERMINATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus SERVICES_RUNNING =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;

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
    private ArgumentCaptor<InstanceMetaData> instanceMetaDataCaptor;

    @Test
    public void saveInstanceMetaDataTestShouldNotSaveInstancesWhenImageNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED, SUBNET_ID, AVAILABILITY_ZONE);
        CloudbreakImageNotFoundException exception = new CloudbreakImageNotFoundException("Image does not exist");
        doThrow(exception).when(imageService).getImage(STACK_ID);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED));

        assertThat(cloudbreakServiceException).hasMessage("Instance metadata collection failed");
        assertThat(cloudbreakServiceException.getCause()).isSameAs(exception);
    }

    static Object[][] saveInstanceMetaDataTestDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone rackIdExpected
                {"subnetId=null, availabilityZone=null", null, null, "/default-rack"},
                {"subnetId=\"\", availabilityZone=null", "", null, "/default-rack"},
                {"subnetId=null, availabilityZone=\"\"", null, "", "/default-rack"},
                {"subnetId=\"\", availabilityZone=\"\"", "", "", "/default-rack"},
                {"subnetId=SUBNET_ID, availabilityZone=null", SUBNET_ID, null, "/SUBNET_ID"},
                {"subnetId=SUBNET_ID, availabilityZone=\"\"", SUBNET_ID, "", "/SUBNET_ID"},
                {"subnetId=null, availabilityZone=AVAILABILITY_ZONE", null, AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
                {"subnetId=\"\", availabilityZone=AVAILABILITY_ZONE", "", AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
                {"subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE", SUBNET_ID, AVAILABILITY_ZONE, "/AVAILABILITY_ZONE"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceMetaDataTestDataProvider")
    public void saveInstanceMetaDataTestOneNewInstance(String testCaseName, String subnetId, String availabilityZone, String rackIdExpected)
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED, subnetId, availabilityZone);

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);

        assertEquals(1, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertCommonProperties(instanceMetaData, subnetId, availabilityZone, rackIdExpected);
        assertEquals(CREATED, instanceMetaData.getInstanceStatus());
        assertNotNull(instanceMetaData.getImage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceMetaDataTestDataProvider")
    public void saveInstanceMetaDataTestOneTerminatedInstance(String testCaseName, String subnetId, String availabilityZone, String rackIdExpected)
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.TERMINATED, subnetId, availabilityZone);

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(0, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertCommonProperties(instanceMetaData, subnetId, availabilityZone, rackIdExpected);
        assertEquals(TERMINATED, instanceMetaData.getInstanceStatus());
        assertNull(instanceMetaData.getImage());
        assertFalse(instanceMetaData.getClusterManagerServer());
    }

    @Test
    public void saveInstanceMetaDataTestServerFlagIsAlreadySet() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.STOPPED, "subnetId", "availabilityZone");
        InstanceMetaData originalInstanceMetadata = new InstanceMetaData();
        originalInstanceMetadata.setServer(true);
        originalInstanceMetadata.setInstanceId("instanceId");
        originalInstanceMetadata.setPrivateId(PRIVATE_ID);
        originalInstanceMetadata.setInstanceGroup(instanceGroup);
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(originalInstanceMetadata));

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(0, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertTrue(instanceMetaData.getAmbariServer());
        assertTrue(instanceMetaData.getClusterManagerServer());
    }

    private Image getEmptyImage() {
        return new Image(null, null, null, null, null, null, null, null);
    }

    private Iterable<CloudVmMetaDataStatus> getCloudVmMetaDataStatuses(InstanceStatus instanceStatus, String subnetId, String availabilityZone) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(null, GROUP_NAME, PRIVATE_ID, List.of(), null, Map.of(), null, null);
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, subnetId);
        params.put(CloudInstance.AVAILABILITY_ZONE, availabilityZone);
        params.put(CloudInstance.INSTANCE_NAME, INSTANCE_NAME);
        CloudInstance cloudInstance = new CloudInstance(null, instanceTemplate, null, params);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, instanceStatus);
        CloudInstanceMetaData cloudInstanceMetaData =
                new CloudInstanceMetaData(PRIVATE_IP, PUBLIC_IP, SSH_PORT, LOCALITY_INDICATOR, CloudInstanceLifeCycle.SPOT);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
        return List.of(cloudVmMetaDataStatus);
    }

    private void assertCommonProperties(InstanceMetaData instanceMetaData, String subnetIdExpected, String availabilityZoneExpected, String rackIdExpected) {
        assertEquals(PRIVATE_IP, instanceMetaData.getPrivateIp());
        assertEquals(PUBLIC_IP, instanceMetaData.getPublicIp());
        assertEquals(SSH_PORT, instanceMetaData.getSshPort());
        assertEquals(LOCALITY_INDICATOR, instanceMetaData.getLocalityIndicator());
        assertEquals(INSTANCE_GROUP_ID, instanceMetaData.getInstanceGroup().getId());
        assertNull(instanceMetaData.getInstanceId());
        assertEquals(PRIVATE_ID, instanceMetaData.getPrivateId());
        assertEquals(CURRENT_TIME, instanceMetaData.getStartDate());
        assertEquals(subnetIdExpected, instanceMetaData.getSubnetId());
        assertThat(instanceMetaData.getAvailabilityZone()).isEqualTo(availabilityZoneExpected);
        assertThat(instanceMetaData.getRackId()).isEqualTo(rackIdExpected);
        assertEquals(INSTANCE_NAME, instanceMetaData.getInstanceName());
        assertEquals(Boolean.FALSE, instanceMetaData.getAmbariServer());
        assertEquals(Boolean.FALSE, instanceMetaData.getClusterManagerServer());
        assertEquals(InstanceMetadataType.CORE, instanceMetaData.getInstanceMetadataType());
        assertEquals(InstanceLifeCycle.SPOT, instanceMetaData.getLifeCycle());
    }

}