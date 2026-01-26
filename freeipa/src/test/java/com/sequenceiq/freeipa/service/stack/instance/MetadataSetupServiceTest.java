package com.sequenceiq.freeipa.service.stack.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@ExtendWith(MockitoExtension.class)
class MetadataSetupServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final Long PRIVATE_ID = 2L;

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String PRIVATE_IP = "PRIVATE_IP";

    private static final String PUBLIC_IP = "PUBLIC_IP";

    private static final Integer SSH_PORT = 22;

    private static final String LOCALITY_INDICATOR = "LOCALITY_INDICATOR";

    private static final Long INSTANCE_GROUP_ID = 3L;

    private static final Long CURRENT_TIME = System.currentTimeMillis();

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private Clock clock;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private MetadataSetupService underTest;

    @Test
    void testCleanupRequestedInstances() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);

        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        when(instanceMetaData1.getInstanceStatus()).thenReturn(InstanceStatus.REQUESTED);
        when(instanceMetaData2.getInstanceStatus()).thenReturn(InstanceStatus.CREATED);
        Set<InstanceMetaData> metadata = Set.of(instanceMetaData1, instanceMetaData2);

        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        when(instanceMetaDataService.findNotTerminatedForStack(any())).thenReturn(metadata);

        underTest.cleanupRequestedInstances(stack);

        verify(instanceMetaData1, times(1)).setTerminationDate(CURRENT_TIME);
        verify(instanceMetaData1, times(1)).setInstanceStatus(InstanceStatus.TERMINATED);
        verify(instanceMetaData2, never()).setTerminationDate(any());
        verify(instanceMetaData2, never()).setInstanceStatus(InstanceStatus.TERMINATED);
        verify(instanceMetaDataService, times(1)).saveAll(any());
    }

    @Test
    void testSaveInstanceMetaData() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Map<String, String> data = Map.of("1", "instance1", "2", "instance2", "3", "instance3");

        Map<String, InstanceMetaData> instances = data.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
            InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
            when(instanceMetaData.getInstanceId()).thenReturn(entry.getValue());
            when(instanceMetaData.getPrivateId()).thenReturn(Long.valueOf(entry.getKey()));
            if (entry.getKey().equals("3")) {
                when(instanceMetaData.getAvailabilityZone()).thenReturn(entry.getKey());
            }
            return instanceMetaData;
        }));
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatusList = data.entrySet().stream().map(entry -> {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = mock(CloudVmMetaDataStatus.class);
            CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
            CloudInstanceMetaData cloudInstanceMetaData = mock(CloudInstanceMetaData.class);
            when(cloudVmMetaDataStatus.getCloudVmInstanceStatus()).thenReturn(cloudVmInstanceStatus);
            when(cloudVmMetaDataStatus.getMetaData()).thenReturn(cloudInstanceMetaData);
            CloudInstance cloudInstance = mock(CloudInstance.class);
            when(cloudVmInstanceStatus.getCloudInstance()).thenReturn(cloudInstance);
            InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
            when(instanceTemplate.getPrivateId()).thenReturn(Long.valueOf(entry.getKey()));
            when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
            when(cloudInstance.getInstanceId()).thenReturn(entry.getValue());
            lenient().when(cloudInstance.getAvailabilityZone()).thenReturn(entry.getKey());
            return cloudVmMetaDataStatus;
        }).collect(Collectors.toList());

        when(instanceMetaDataService.findNotTerminatedForStack(any())).thenReturn(new HashSet<>(instances.values()));
        underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatusList, null);
        instances.entrySet().stream().forEach(entry -> {
            verify(entry.getValue(), times(entry.getValue().getAvailabilityZone() != null ? 0 : 1)).setAvailabilityZone(entry.getKey());
        });
    }
}