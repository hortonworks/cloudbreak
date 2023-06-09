package com.sequenceiq.environment.network.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentNetworkConverterTest {

    private static final String NETWORK_ID = "networkId";

    private static final String PROJECT_ID = "projectId";

    private static final String LOCATION = "location1";

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private GcpEnvironmentNetworkConverter underTest;

    @Test
    void getCloudPlatform() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.GCP);
    }

    @Test
    void convertToNetwork() {
        GcpNetwork gcpNetwork = new GcpNetwork();
        gcpNetwork.setNetworkId(NETWORK_ID);
        gcpNetwork.setSharedProjectId(PROJECT_ID);
        gcpNetwork.setNoPublicIp(true);
        gcpNetwork.setNoFirewallRules(true);
        EnvironmentView env = new EnvironmentView();
        env.setLocation(LOCATION);
        gcpNetwork.setEnvironment(env);
        gcpNetwork.setSubnetMetas(Map.of("subnet1", new CloudSubnet("id1", "subnet1"),
                "subnet2", new CloudSubnet("id2", "subnet2")));
        gcpNetwork.setEndpointGatewaySubnetMetas(Map.of("subnet3", new CloudSubnet("id3", "subnet3"),
                "subnet4", new CloudSubnet("id4", "subnet4")));
        Set<String> availabilityZones = Set.of("gcp-region1-zone1");
        gcpNetwork.setAvailabilityZones(availabilityZones);

        Network result = underTest.convertToNetwork(gcpNetwork);

        assertThat(result.getParameters())
                .containsEntry(GcpStackUtil.NETWORK_ID, NETWORK_ID)
                .containsEntry(GcpStackUtil.SHARED_PROJECT_ID, PROJECT_ID)
                .containsEntry(GcpStackUtil.NO_PUBLIC_IP, Boolean.TRUE)
                .containsEntry(GcpStackUtil.NO_FIREWALL_RULES, Boolean.TRUE)
                .containsEntry(GcpStackUtil.REGION, LOCATION)
                .anySatisfy((k, v) -> {
                    assertThat(k).isEqualTo(NetworkConstants.SUBNET_ID);
                    assertThat(v).satisfiesAnyOf(v2 -> assertThat(v2).isEqualTo("subnet1"),
                            v2 -> assertThat(v2).isEqualTo("subnet2"));
                })
                .anySatisfy((k, v) -> {
                    assertThat(k).isEqualTo(NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID);
                    assertThat(v).satisfiesAnyOf(v2 -> assertThat(v2).isEqualTo("subnet3"),
                            v2 -> assertThat(v2).isEqualTo("subnet4"));
                })
                .containsEntry(NetworkConstants.AVAILABILITY_ZONES, availabilityZones);
    }
}
