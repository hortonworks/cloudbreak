package com.sequenceiq.environment.environment.service.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

public class NetworkTest {

    protected static final String UNMATCHED_AZ_MSG = "Please provide public subnets in each of the following availability zones:";

    protected static final String AZ_1 = "AZ-1";

    protected static final String AZ_2 = "AZ-2";

    protected static final String ID_1 = "id1";

    protected static final String ID_2 = "id2";

    protected static final String PUBLIC_ID_1 = "public-id1";

    protected static final String PUBLIC_ID_2 = "public-id2";

    protected static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    protected EnvironmentDto createEnvironmentDto() {
        NetworkDto networkDto = NetworkDto.builder()
            .build();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(123L);
        environmentDto.setName("name");
        environmentDto.setNetwork(networkDto);
        return environmentDto;
    }

    protected AwsNetwork createNetwork() {
        AwsNetwork network = new AwsNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        network.setRegistrationType(RegistrationType.EXISTING);
        return network;
    }

    protected Environment createEnvironment(BaseNetwork network) {
        Environment environment = new Environment();
        environment.setName("name");
        environment.setAccountId("1234");
        environment.setNetwork(network);
        environment.setCloudPlatform("AWS");
        return environment;
    }

    protected CloudSubnet createPrivateSubnet(String id, String aZ) {
        return new CloudSubnet(id, "name", aZ, "cidr", true, false, false, SubnetType.PRIVATE);
    }

    protected CloudSubnet createPublicSubnet(String id, String aZ) {
        return new CloudSubnet(id, "name", aZ, "cidr", false, true, true, SubnetType.PUBLIC);
    }

    protected Map<String, CloudSubnet> createDefaultPrivateSubnets() {
        Map<String, CloudSubnet> subnets = new HashMap<>();
        subnets.put(ID_1, createPrivateSubnet(ID_1, AZ_1));
        subnets.put(ID_2, createPrivateSubnet(ID_2, AZ_2));
        return subnets;
    }

    protected Map<String, CloudSubnet> createDefaultPublicSubnets() {
        Map<String, CloudSubnet> subnets = new HashMap<>();
        subnets.put(PUBLIC_ID_1, createPublicSubnet(PUBLIC_ID_1, AZ_1));
        subnets.put(PUBLIC_ID_2, createPublicSubnet(PUBLIC_ID_2, AZ_2));
        return subnets;
    }

    protected Map<String, CloudSubnet> createPrivateSubnetsWithInternetRouting() {
        Map<String, CloudSubnet> subnets = new HashMap<>();
        CloudSubnet subnet1 = createPrivateSubnet(ID_1, AZ_1);
        subnet1.setRoutableToInternet(true);
        subnets.put(ID_1, subnet1);
        CloudSubnet subnet2 = createPrivateSubnet(ID_2, AZ_2);
        subnet2.setRoutableToInternet(true);
        subnets.put(ID_2, subnet2);
        return subnets;
    }
}
