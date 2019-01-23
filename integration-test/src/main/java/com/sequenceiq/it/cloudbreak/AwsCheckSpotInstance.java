package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter.GetStackByNameV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.it.IntegrationTestContext;

public class AwsCheckSpotInstance extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull("Stack id is mandatory.", itContext.getContextParam(CloudbreakITContextConstants.STACK_ID));
    }

    @Parameters({"region", "hostGroupToCheck", "scalingAdjustment"})
    @Test
    public void checkSpotInstance(Regions region, String hostGroupToCheck, @Optional Integer scalingAdjustment) {
        //GIVEN
        Integer spotInstanceCount = 0;
        IntegrationTestContext itContext = getItContext();

        StackV4Response stackResponse = getCloudbreakClient().stackV4Endpoint().get(
                itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class),
                "", // TODO figure out how to find the name of the stack
                new GetStackByNameV4Filter());

        List<InstanceGroupV4Response> instanceGroups = stackResponse.getInstanceGroups();

        Collection<String> instanceIdList = new ArrayList<>();
        List<String> hostGroupList = Arrays.asList(hostGroupToCheck.split(","));

        for (InstanceGroupV4Response instanceGroup : instanceGroups) {
            if (hostGroupList.contains(instanceGroup.getMetadata().getGroup())) {
                Set<InstanceMetaDataJson> instanceMetaData = instanceGroup.getMetadata();
                for (InstanceMetaDataJson metaData : instanceMetaData) {
                    instanceIdList.add(metaData.getInstanceId());
                }
            }
        }
        //WHEN
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();
        DescribeSpotInstanceRequestsResult describeSpotInstanceRequestsResult = ec2.describeSpotInstanceRequests();
        List<SpotInstanceRequest> spotInstanceRequests = describeSpotInstanceRequestsResult.getSpotInstanceRequests();
        //THEN
        Assert.assertFalse(spotInstanceRequests.isEmpty());

        Collection<String> spotInstanceIdList = new ArrayList<>();

        for (SpotInstanceRequest request : spotInstanceRequests) {
            spotInstanceIdList.add(request.getInstanceId());
        }

        for (String id : instanceIdList) {
            Assert.assertTrue(spotInstanceIdList.contains(id));
            if (spotInstanceIdList.contains(id)) {
                spotInstanceCount += 1;
            }
        }

        if (scalingAdjustment != null) {
            Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class));
            Integer instanceCountPrev = 0;
            for (String hostGroup : hostGroupList) {
                List<Map<String, Integer>> instanceList = itContext.getContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, List.class);
                Assert.assertTrue(instanceList.size() >= 2);
                instanceCountPrev += instanceList.get(instanceList.size() - 2).get(hostGroup);
            }
            Assert.assertEquals(Integer.valueOf(instanceCountPrev + scalingAdjustment), spotInstanceCount);
        }
    }
}