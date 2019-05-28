package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;

@Component
public class AwsCloudProvider extends AbstractCloudProvider {

    private static final String KEY_BASED_CREDENTIAL = "key";

    @Inject
    private AwsProperties awsProperties;

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(awsProperties.getInstance().getType());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withAws(stackParameters());
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    public AwsStackV4Parameters stackParameters() {
        return new AwsStackV4Parameters();
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = awsProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = awsProperties.getInstance().getVolumeCount();
        String attachedVolumeType = awsProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return network.withSubnetCIDR(getSubnetCIDR())
                .withAws(networkParameters());
    }

    private AwsNetworkV4Parameters networkParameters() {
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setVpcId(getVpcId());
        awsNetworkV4Parameters.setSubnetId(getSubnetId());
        return awsNetworkV4Parameters;
    }

    public String getVpcId() {
        return awsProperties.getVpcId();
    }

    public String getSubnetId() {
        return awsProperties.getSubnetId();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public String region() {
        return awsProperties.getRegion();
    }

    @Override
    public String location() {
        return awsProperties.getLocation();
    }

    @Override
    public String availabilityZone() {
        return awsProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String publicKeyId = awsProperties.getPublicKeyId();
        stackAuthenticationEntity.withPublicKeyId(publicKeyId);
        return stackAuthenticationEntity;
    }

    @Override
    public String getBlueprintName() {
        return awsProperties.getDefaultBlueprintName();
    }
}
