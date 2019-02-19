package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import static com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity.EUROPE;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;

@Component
public class MockCloudProvider extends AbstractCloudProvider {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String MOCK_CLUSTER_DEFAULT_NAME = "autotesting-mock-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    public static final String CREDENTIAL_DEFAULT_NAME = "autotesting-mock-cred";

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting mock credential";

    public static final String CLUSTER_DEFINITION_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    public static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    public static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    public static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    public static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    public static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    public static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting mock network";

    @Inject
    private TestParameter testParameter;

    @Override
    public String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = getTestParameter().get("mockAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public String region() {
        String regionParam = getTestParameter().get("mockRegion");

        return regionParam == null ? EUROPE : regionParam;
    }

    @Override
    public InstanceTemplateV4Entity template(TestContext testContext) {
        String instanceTypeDefaultValue = "large";
        String instanceTypeParam = getTestParameter().get("mockInstanceType");

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("mockInstanceVolumeCount");

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("mockInstanceVolumeSize");

        String volumeTypeDefault = "magnetic";
        String volumeTypeParam = getTestParameter().get("mockInstanceVolumeType");

        var volume = new VolumeV4Request();
        volume.setCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));
        volume.setSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));
        volume.setType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        return testContext.init(InstanceTemplateV4Entity.class)
                .withInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam)
                .withAttachedVolumes(Set.of(volume));
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("mockVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("mockSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getInternetGatewayId() {
        String gatewayId = getTestParameter().get("mockInternetGatewayId");
        return gatewayId == null ? INTERNET_GATEWAY_ID : gatewayId;
    }

    @Override
    public Object networkProperties() {
        var parameters = new MockNetworkV4Parameters();
        parameters.setInternetGatewayId(getInternetGatewayId());
        parameters.setVpcId(getVpcId());
        return parameters;
    }

    @Override
    public Object subnetProperties() {
        var parameters = new MockNetworkV4Parameters();
        parameters.setSubnetId(getSubnetId());
        parameters.setVpcId(getVpcId());
        return parameters;
    }

    @Override
    public NetworkV2Entity newNetwork(TestContext testContext) {
        return testContext.init(NetworkV2Entity.class)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public NetworkV2Entity existingNetwork(TestContext testContext) {
        var network = testContext.init(NetworkV2Entity.class)
                .withSubnetCIDR(getSubnetCIDR());
        network.getRequest().setMock((MockNetworkV4Parameters) networkProperties());
        return network;
    }

    @Override
    public NetworkV2Entity existingSubnet(TestContext testContext) {
        var network = testContext.given(NetworkV2Entity.class);
        network.getRequest().setMock((MockNetworkV4Parameters) subnetProperties());
        return network;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }
}