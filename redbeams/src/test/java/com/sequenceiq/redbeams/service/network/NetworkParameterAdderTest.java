package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

public class NetworkParameterAdderTest {

    private static final String TEST_VPC_CIDR = "1.2.3.4/16";

    private static final String TEST_VPC_ID = "vpcId";

    private final NetworkParameterAdder underTest = new NetworkParameterAdder();

    @Test
    public void testAddNetworkParametersWhenAws() {
        Map<String, Object> parameters = new HashMap<>();
        List<String> subnetIds = List.of("subnet1", "subnet2");

        parameters = underTest.addSubnetIds(parameters, subnetIds, CloudPlatform.AWS);

        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.SUBNET_ID, String.join(",", subnetIds)));
    }

    @Test
    public void testAddParametersWhenAws() {
        Map<String, Object> parameters = new HashMap<>();
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withSecurityAccess(SecurityAccessResponse.builder()
                        .withCidr(TEST_VPC_CIDR).build())
                .withNetwork(EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                        .withAws(EnvironmentNetworkAwsParamsBuilder.anEnvironmentNetworkAwsParams().withVpcId(TEST_VPC_ID).build())
                        .build())
                .build();

        parameters = underTest.addParameters(parameters, environment, CloudPlatform.AWS);

        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.VPC_ID, TEST_VPC_ID));
        assertThat(parameters, IsMapContaining.hasEntry(NetworkParameterAdder.VPC_CIDR, TEST_VPC_CIDR));
    }
}
