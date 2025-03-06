package com.sequenceiq.freeipa.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class ProviderCheckerTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackInstanceProviderChecker stackInstanceProviderChecker;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private ProviderChecker underTest;

    @Test
    void testCheckProviderWhenUnknown() {
        when(stackInstanceProviderChecker.checkStatus(any(), anySet()))
                .thenReturn(List.of(cloudVmInstanceStatus("instanceId1"), cloudVmInstanceStatus("instanceId2")));
        List<ProviderSyncResult> syncResults = underTest.updateAndGetStatuses(new Stack(),
                Set.of(instanceMetadata("instanceId1"), instanceMetadata("instanceId2")), new HashMap<>(), true);

        assertThat(syncResults).extracting(ProviderSyncResult::getStatus)
                .containsOnly(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.UNKNOWN);
        assertThat(syncResults).extracting(ProviderSyncResult::getInstanceId).containsExactlyInAnyOrder("instanceId1", "instanceId2");
    }

    private CloudVmInstanceStatus cloudVmInstanceStatus(String instanceId) {
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null, null, null), InstanceStatus.UNKNOWN, "access denied");
    }

    private InstanceMetaData instanceMetadata(String instanceId) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(instanceId);
        return instanceMetaData;
    }

}