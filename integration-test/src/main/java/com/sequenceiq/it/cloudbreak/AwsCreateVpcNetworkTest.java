package com.sequenceiq.it.cloudbreak;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;

public class AwsCreateVpcNetworkTest extends AbstractCloudbreakIntegrationTest {

    private static final List<StackStatus> FAILED_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCreateVpcNetworkTest.class);
    private static final int MAX_TRY = 30;

    @Test
    @Parameters({ "networkName", "description", "publicInAccount", "regionName", "vpcStackName", "vpcName", "existingSubnet" })
    public void createNetwork(String networkName, @Optional("") String description, @Optional("false") boolean publicInAccount,
            String regionName, @Optional("it-vpc-stack") String vpcStackName, @Optional("it-vpc") String vpcName, boolean existingSubnet) {
        AmazonCloudFormationClient client = new AmazonCloudFormationClient();
        client.setRegion(RegionUtils.getRegion(regionName));

        Map<String, Object> networkMap = new HashMap<>();

        String vpcCreationJson = existingSubnet ?  "public_vpc_with_subnet.json" : "public_vpc_wihout_subnet.json";

        try (InputStream vpcJsonInputStream = getClass().getResourceAsStream("/cloudformation/" + vpcCreationJson)) {
            String vpcCFTemplateString = IOUtils.toString(vpcJsonInputStream);
            CreateStackRequest stackRequest = createStackRequest(vpcStackName, vpcName, vpcCFTemplateString);
            client.createStack(stackRequest);

            List<Output> outputForRequest = getOutputForRequest(vpcStackName, client);
            if (existingSubnet) {
                networkMap.put("vpcId", outputForRequest.get(0).getOutputValue());
                networkMap.put("subnetId", outputForRequest.get(1).getOutputValue());
            } else {
                networkMap.put("vpcId", outputForRequest.get(1).getOutputValue());
                networkMap.put("internetGatewayId", outputForRequest.get(0).getOutputValue());
            }
        } catch (IOException e) {
            LOGGER.error("can't read vpc cloudformation template file");
            throw new RuntimeException(e);
        }

        NetworkJson networkJson = new NetworkJson();
        networkJson.setName(networkName);
        networkJson.setDescription(description);
        networkJson.setParameters(networkMap);
        if (!existingSubnet) {
            networkJson.setSubnetCIDR("10.0.0.0/24");
        }
        networkJson.setCloudPlatform("AWS");
        networkJson.setPublicInAccount(publicInAccount);
        String id = getCloudbreakClient().networkEndpoint().postPrivate(networkJson).getId().toString();
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }

    private List<Output> getOutputForRequest(String vpcStackName, AmazonCloudFormationClient client) {
        int tried = 0;
        while (tried < MAX_TRY) {
            LOGGER.info("checking vpc stack creation result, tried: " + tried + "/" + MAX_TRY);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
            describeStacksRequest.withStackName(vpcStackName);
            Stack resultStack = client.describeStacks(describeStacksRequest).getStacks().get(0);
            StackStatus stackStatus = StackStatus.valueOf(resultStack.getStackStatus());
            if (FAILED_STATUSES.contains(stackStatus)) {
                LOGGER.error("stack creation failed: ", stackStatus);
                throw new RuntimeException();
            } else if (CREATE_COMPLETE.equals(stackStatus)) {
                return resultStack.getOutputs();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                LOGGER.error("thread sleep interrupted", e);
            }
            tried++;
        }
        throw new RuntimeException("vpc creation timed out");
    }

    private CreateStackRequest createStackRequest(String vpcStackName, String vpcName, String vpcCFTemplateString) {
        CreateStackRequest createStackRequest = new CreateStackRequest();
        Parameter vpcNameParameter = new Parameter();
        vpcNameParameter.setParameterKey("VpcName");
        vpcNameParameter.setParameterValue(vpcName);
        createStackRequest.withParameters(vpcNameParameter);
        createStackRequest.withStackName(vpcStackName);
        createStackRequest.withTemplateBody(vpcCFTemplateString);
        return createStackRequest;
    }
}
