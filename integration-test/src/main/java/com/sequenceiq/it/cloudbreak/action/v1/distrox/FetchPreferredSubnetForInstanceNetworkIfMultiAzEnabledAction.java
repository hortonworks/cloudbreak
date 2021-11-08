package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.ArrayList;

import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

public class FetchPreferredSubnetForInstanceNetworkIfMultiAzEnabledAction implements Action<DistroXTestDto, CloudbreakClient> {

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if ("AWS_NATIVE".equals(testDto.getRequest().getVariant())) {
            EnvironmentClient envClient = testContext.getMicroserviceClient(EnvironmentClient.class);
            DetailedEnvironmentResponse envResponse = envClient.getDefaultClient()
                    .environmentV1Endpoint()
                    .getByName(testDto.getRequest().getEnvironmentName());
            InstanceGroupNetworkV1Request instanceGroupNetworkV1Request = new InstanceGroupNetworkV1Request();
            InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
            awsNetworkV1Parameters.setSubnetIds(new ArrayList<>(envResponse.getNetwork().getPreferedSubnetIds()));
            instanceGroupNetworkV1Request.setAws(awsNetworkV1Parameters);
            testDto.getRequest().getInstanceGroups().forEach(s -> s.setNetwork(instanceGroupNetworkV1Request));
        }
        return testDto;
    }
}
