package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class NetworkV1ToNetworkV4ConverterTest {

    private static final String VPC_ID = "vpc-123";

    private static final String GROUP_NAME = "group-123";

    private static final String SUBNET_ID = "subnet-123";

    private static final Set<String> SUBNET_IDS = Sets.newHashSet("subnet-123", "subnet-456");

    @InjectMocks
    private NetworkV1ToNetworkV4Converter underTest;

    @Test
    public void testConvertToStackRequestWhenAwsPresentedWithSubnet() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        Assert.assertEquals(networkV4Request.createAws().getVpcId(), VPC_ID);
        Assert.assertEquals(networkV4Request.createAws().getSubnetId(), SUBNET_ID);
    }

    @Test
    public void testConvertToStackRequestWhenAwsPresentedWithoutSubnet() {
        NetworkV1Request networkV1Request = awsEmptyNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        Assert.assertEquals(networkV4Request.createAws().getVpcId(), VPC_ID);
        Assert.assertTrue(SUBNET_IDS.contains(networkV4Request.createAws().getSubnetId()));
    }

    @Test
    public void testConvertToStackRequestWhenAzurePresentedWithSubnet() {
        NetworkV1Request networkV1Request = azureNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = azureEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        Assert.assertEquals(networkV4Request.createAzure().getNetworkId(), VPC_ID);
        Assert.assertEquals(networkV4Request.createAzure().getResourceGroupName(), GROUP_NAME);
        Assert.assertEquals(networkV4Request.createAzure().getSubnetId(), SUBNET_ID);
    }

    @Test
    public void testConvertToStackRequestWhenAzurePresentedWithoutSubnet() {
        NetworkV1Request networkV1Request = azureEmptyNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = azureEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        Assert.assertEquals(networkV4Request.createAzure().getNetworkId(), VPC_ID);
        Assert.assertEquals(networkV4Request.createAzure().getResourceGroupName(), GROUP_NAME);
        Assert.assertTrue(SUBNET_IDS.contains(networkV4Request.createAzure().getSubnetId()));
    }

    @Test
    public void testConvertToStackRequestWhenYarnPresented() {
        NetworkV1Request networkV1Request = yarnNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = yarnEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        Assert.assertTrue(networkV4Request.createYarn().asMap().size() == 1);
    }

    private DetailedEnvironmentResponse awsEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("AWS");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);

        EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkAwsParams.setVpcId(VPC_ID);

        environmentNetworkResponse.setAws(environmentNetworkAwsParams);

        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request awsNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AwsNetworkV1Parameters awsNetworkV1Parameters = new AwsNetworkV1Parameters();
        awsNetworkV1Parameters.setSubnetId(SUBNET_ID);

        networkV1Request.setAws(awsNetworkV1Parameters);

        return networkV1Request;
    }

    private NetworkV1Request awsEmptyNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AwsNetworkV1Parameters awsNetworkV1Parameters = new AwsNetworkV1Parameters();

        networkV1Request.setAws(awsNetworkV1Parameters);

        return networkV1Request;
    }

    private DetailedEnvironmentResponse azureEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("AZURE");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);

        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setNetworkId(VPC_ID);
        environmentNetworkAzureParams.setResourceGroupName(GROUP_NAME);


        environmentNetworkResponse.setAzure(environmentNetworkAzureParams);

        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request azureNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AzureNetworkV1Parameters azureNetworkV1Parameters = new AzureNetworkV1Parameters();
        azureNetworkV1Parameters.setSubnetId(SUBNET_ID);

        networkV1Request.setAzure(azureNetworkV1Parameters);

        return networkV1Request;
    }

    private NetworkV1Request azureEmptyNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AzureNetworkV1Parameters azureNetworkV1Parameters = new AzureNetworkV1Parameters();

        networkV1Request.setAzure(azureNetworkV1Parameters);

        return networkV1Request;
    }

    private DetailedEnvironmentResponse yarnEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("YARN");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();

        EnvironmentNetworkYarnParams environmentNetwork = new EnvironmentNetworkYarnParams();
        environmentNetwork.setQueue("default");


        environmentNetworkResponse.setYarn(environmentNetwork);
        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request yarnNetworkV1Request() {
        return new NetworkV1Request();
    }

}