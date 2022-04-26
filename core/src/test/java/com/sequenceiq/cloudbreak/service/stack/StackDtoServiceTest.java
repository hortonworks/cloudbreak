package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
public class StackDtoServiceTest {

    @InjectMocks
    private StackDtoService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Test
    public void test() {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceGroup(new InstanceGroup());
        instanceMetaDatas.add(instanceMetaData1);
        instanceMetaDatas.add(instanceMetaData2);
        instanceMetaDatas.add(instanceMetaData3);

        StackListItem stack = mock(StackListItem.class);
        when(stack.getId()).thenReturn(1L);
        when(stackRepository.findByWorkspaceId(1L, "envCrn", StackType.WORKLOAD)).thenReturn(Optional.of(stack));
        when(instanceMetaDataService.findNotTerminatedAsOrderedListForStack(1L)).thenReturn(instanceMetaDatas);
        underTest.get(1L, "envCrn", StackType.WORKLOAD);
    }
}
