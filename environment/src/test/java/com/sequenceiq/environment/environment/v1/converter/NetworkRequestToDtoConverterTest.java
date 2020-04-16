package com.sequenceiq.environment.environment.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(SpringExtension.class)
public class NetworkRequestToDtoConverterTest {

    private static final String SUBNET_ID = "subnet-id";

    private static final String NETWORK_ID = "network-id";

    private final NetworkRequestToDtoConverter underTest = new NetworkRequestToDtoConverter();

    @Test
    void testConvertAws() {
        EnvironmentNetworkRequest network = createNetworkRequest();
        network.setAws(createAwsParams());

        NetworkDto actual = underTest.convert(network);

        assertEquals(network.getAws().getVpcId(), actual.getAws().getVpcId());
        assertEquals(network.getAws().getVpcId(), actual.getNetworkId());
        assertCommonFields(network, actual);
    }

    @Test
    void testConvertAzure() {
        EnvironmentNetworkRequest network = createNetworkRequest();
        network.setAzure(createAzureParams());

        NetworkDto actual = underTest.convert(network);

        assertEquals(network.getAzure().getNetworkId(), actual.getAzure().getNetworkId());
        assertEquals(network.getAzure().getResourceGroupName(), actual.getAzure().getResourceGroupName());
        assertEquals(network.getAzure().getNoPublicIp(), actual.getAzure().isNoPublicIp());
        assertCommonFields(network, actual);
    }

    @Test
    void testConvertYarn() {
        EnvironmentNetworkRequest network = createNetworkRequest();
        network.setYarn(createYarnParams());

        NetworkDto actual = underTest.convert(network);

        assertEquals(network.getYarn().getQueue(), actual.getYarn().getQueue());
        assertCommonFields(network, actual);
    }

    @Test
    void testConvertMock() {
        EnvironmentNetworkRequest network = createNetworkRequest();
        network.setMock(createMockParams());

        NetworkDto actual = underTest.convert(network);

        assertEquals(network.getMock().getVpcId(), actual.getMock().getVpcId());
        assertEquals(network.getMock().getInternetGatewayId(), actual.getMock().getInternetGatewayId());
        assertCommonFields(network, actual);
    }

    private EnvironmentNetworkRequest createNetworkRequest() {
        EnvironmentNetworkRequest request = new EnvironmentNetworkRequest();
        request.setNetworkCidr("10.10.10.10/16");
        request.setSubnetIds(Set.of(SUBNET_ID));
        request.setPrivateSubnetCreation(PrivateSubnetCreation.ENABLED);
        return request;
    }

    private EnvironmentNetworkAwsParams createAwsParams() {
        EnvironmentNetworkAwsParams awsParams = new EnvironmentNetworkAwsParams();
        awsParams.setVpcId(NETWORK_ID);
        return awsParams;
    }

    private EnvironmentNetworkMockParams createMockParams() {
        EnvironmentNetworkMockParams mockParams = new EnvironmentNetworkMockParams();
        mockParams.setInternetGatewayId("internet-gateway-id");
        mockParams.setVpcId(NETWORK_ID);
        return mockParams;
    }

    private EnvironmentNetworkAzureParams createAzureParams() {
        EnvironmentNetworkAzureParams azureParams = new EnvironmentNetworkAzureParams();
        azureParams.setNetworkId(NETWORK_ID);
        azureParams.setResourceGroupName("resource-group");
        azureParams.setNoPublicIp(true);
        return azureParams;
    }

    private EnvironmentNetworkYarnParams createYarnParams() {
        EnvironmentNetworkYarnParams yarnParams = new EnvironmentNetworkYarnParams();
        yarnParams.setQueue("yarn-queue");
        return yarnParams;
    }

    private void assertCommonFields(EnvironmentNetworkRequest network, NetworkDto actual) {
        assertNotNull(actual.getSubnetMetas().get(SUBNET_ID));
        assertEquals(actual.getSubnetMetas().get(SUBNET_ID).getId(), SUBNET_ID);
        assertEquals(actual.getSubnetMetas().get(SUBNET_ID).getName(), SUBNET_ID);
        assertEquals(network.getPrivateSubnetCreation(), actual.getPrivateSubnetCreation());
        assertEquals(network.getNetworkCidr(), actual.getNetworkCidr());
    }

}