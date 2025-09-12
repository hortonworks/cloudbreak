package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupSubnetCollectorTest {

    private InstanceGroupSubnetCollector underTest;

    @Mock
    private InstanceGroup instanceGroup;

    @Mock
    private InstanceGroupNetwork instanceGroupNetwork;

    @Mock
    private Network network;

    @Mock
    private Json attributes;

    @BeforeEach
    public void setUp() {
        underTest = new InstanceGroupSubnetCollector();
    }

    @Test
    public void collectReturnsEmptySetWhenAttributesAreNull() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(null);

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).isEmpty();
    }

    @Test
    public void collectReturnsStackSubnetWhenInstanceGroupNetworkIsNull() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(null);
        when(network.getAttributes()).thenReturn(new Json(Map.of(SUBNET_ID, "subnet-1")));

        Set<String> result = underTest.collect(instanceGroup, network);

        assertThat(result).containsExactlyInAnyOrder("subnet-1");
    }

    @Test
    public void collectReturnsEmptySetWhenAttributesMapIsEmpty() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(Collections.emptyMap());

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).isEmpty();
    }

    @Test
    public void collectReturnsSubnetsAndEndpointGatewaySubnetIds() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(Map.of(
                NetworkConstants.SUBNET_IDS, List.of("subnet-1", "subnet-2"),
                NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS, List.of("subnet-3", "subnet-4")
        ));

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).containsExactlyInAnyOrder("subnet-1", "subnet-2");
    }

    @Test
    public void collectHandlesMissingSubnetIdsGracefully() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(Map.of(
                NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS, List.of("subnet-1")
        ));

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).containsExactly();
    }

    @Test
    public void collectHandlesMissingEndpointGatewaySubnetIdsGracefully() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(Map.of(
                NetworkConstants.SUBNET_IDS, List.of("subnet-1")
        ));

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).containsExactly("subnet-1");
    }

    @Test
    public void collectHandlesDuplicateSubnets() {
        when(instanceGroup.getInstanceGroupNetwork()).thenReturn(instanceGroupNetwork);
        when(instanceGroupNetwork.getAttributes()).thenReturn(attributes);
        when(attributes.getMap()).thenReturn(Map.of(
                NetworkConstants.SUBNET_IDS, List.of("subnet-1", "subnet-2"),
                NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS, List.of("subnet-1", "subnet-3")
        ));

        Set<String> result = underTest.collect(instanceGroup, null);

        assertThat(result).containsExactlyInAnyOrder("subnet-1", "subnet-2");
    }
}