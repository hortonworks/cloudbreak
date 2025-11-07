package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@ExtendWith(MockitoExtension.class)
class StackHelperTest {
    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @InjectMocks
    private StackHelper underTest;

    @Test
    void testBuildCommandsWithLoadBalancer() throws IOException {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(1L);
        LoadBalancer loadBalancer = new LoadBalancer();
        Set<String> lbIps = new LinkedHashSet<>();
        Collections.addAll(lbIps, "ipaIp1", "ipaIp2", "ipaIp3");
        loadBalancer.setIp(lbIps);
        lenient().when(freeIpaLoadBalancerService.findByStackId(stack.getId())).thenReturn(Optional.of(loadBalancer));
        // WHEN
        List<String> result = underTest.getServerIps(stack);
        // THEN
        Assertions.assertThat(result).containsAll(lbIps);
    }

    @Test
    void testGetServerIpsWithoutLoadBalancer() throws IOException {
        // GIVEN
        Stack stack = mock(Stack.class);
        lenient().when(stack.getId()).thenReturn(1L);
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setPrivateIp("ip1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setPrivateIp("ip2");
        Set<InstanceMetaData> instanceMetadatas = new LinkedHashSet<>();
        Collections.addAll(instanceMetadatas, instanceMetaData1, instanceMetaData2);
        lenient().when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetadatas);
        // WHEN
        List<String> result = underTest.getServerIps(stack);
        // THEN
        assertEquals(List.of("ip1", "ip2"), result);
    }
}
