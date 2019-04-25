package com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

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
    public NetworkV2TestDto network(NetworkV2TestDto network) {
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
    public CredentialTestDto credential(CredentialTestDto credential) {
        String credentialType = awsProperties.getCredential().getType();
        AwsCredentialV4Parameters parameters;
        if (KEY_BASED_CREDENTIAL.equalsIgnoreCase(credentialType)) {
            parameters = awsCredentialDetailsKey();
        } else {
            parameters = awsCredentialDetailsArn();
        }
        return credential
                .withDescription(commonCloudProperties().getDefaultCredentialDescription())
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withAwsParameters(parameters);
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

    public AwsCredentialV4Parameters awsCredentialDetailsArn() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        String roleArn = awsProperties.getCredential().getRoleArn();
        roleBasedCredentialParameters.setRoleArn(roleArn);
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        keyBasedCredentialParameters.setAccessKey(accessKeyId);
        String secretKey = awsProperties.getCredential().getSecretKey();
        keyBasedCredentialParameters.setSecretKey(secretKey);
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }
}
