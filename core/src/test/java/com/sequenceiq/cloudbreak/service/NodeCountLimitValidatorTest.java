package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class NodeCountLimitValidatorTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private LimitConfiguration nodeCountLimitConfiguration;

    @InjectMocks
    private NodeCountLimitValidator underTest;

    @Test
    public void testUpscaleValidation() {
        when(nodeCountLimitConfiguration.getNodeCountLimit()).thenReturn(500);
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(10));
        underTest.validateScale(1L, 1);
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(499));
        assertThrows(BadRequestException.class, () -> underTest.validateScale(1L, 2),
                "The maximum count of nodes for this cluster cannot be higher than 500");
        when(nodeCountLimitConfiguration.getNodeCountLimit()).thenReturn(600);
        when(instanceMetaDataService.countByStackId(anyLong())).thenReturn(count(499));
        underTest.validateScale(1L, 2);
        verify(instanceMetaDataService, times(3)).countByStackId(anyLong());
    }

    @Test
    public void testDownscaleValidation() {
        underTest.validateScale(1L, -1);
        verifyNoInteractions(instanceMetaDataService);
    }

    @Test
    public void testProvisionValidation() {
        when(nodeCountLimitConfiguration.getNodeCountLimit()).thenReturn(500);
        underTest.validateProvision(stackV4Request(499));
        assertThrows(BadRequestException.class, () -> underTest.validateProvision(stackV4Request(501)),
                "The maximum count of nodes for this cluster cannot be higher than 500");
    }

    private StackInstanceCount count(int count) {
        return new StackInstanceCount() {
            @Override
            public Long getStackId() {
                return 1L;
            }

            @Override
            public Integer getInstanceCount() {
                return count;
            }
        };

    }

    private StackV4Request stackV4Request(int nodeCount) {
        StackV4Request stackV4Request = new StackV4Request();
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setNodeCount(nodeCount);
        stackV4Request.setInstanceGroups(Lists.newArrayList(instanceGroupV4Request));
        return stackV4Request;
    }
}
