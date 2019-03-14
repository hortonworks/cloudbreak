package com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters.CREDENTIAL_DEFAULT_DESCRIPTION;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

@Component
public class AwsCloudProvider extends AbstractCloudProvider {

    private static final String KEY_BASED_CREDENTIAL = "key";

    @Override
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return template.withInstanceType(getTestParameter().getWithDefault(AwsParameters.Instance.TYPE, "m5.2xlarge"));
    }

    @Override
    public StackV4EntityBase stack(StackV4EntityBase stack) {
        return stack.withAws(stackParameters());
    }

    @Override
    public AwsStackV4Parameters stackParameters() {
        return new AwsStackV4Parameters();
    }

    @Override
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_COUNT, "1"));
        String attachedVolumeType = getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_TYPE, "gp2");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
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
        return getTestParameter().getWithDefault(AwsParameters.VPC_ID, "vpc-5e68eb3a");
    }

    public String getSubnetId() {
        return getTestParameter().getWithDefault(AwsParameters.SUBNET_ID, "subnet-87b729f1");
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        String credentialType = getTestParameter().getWithDefault(AwsParameters.Credential.TYPE, KEY_BASED_CREDENTIAL);
        AwsCredentialV4Parameters parameters;
        if (KEY_BASED_CREDENTIAL.equalsIgnoreCase(credentialType)) {
            parameters = awsCredentialDetailsKey();
        } else {
            parameters = awsCredentialDetailsArn();
        }
        return credential
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withAwsParameters(parameters);
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault(AwsParameters.REGION, "eu-west-1");
    }

    @Override
    public String location() {
        return getTestParameter().getWithDefault(AwsParameters.LOCATION, "eu-west-1");
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(AwsParameters.AVAILABILITY_ZONE, "eu-west-1a");
    }

    @Override
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
        String publicKeyId = getTestParameter().getWithDefault(AwsParameters.PUBLIC_KEY_ID, "api-e2e-test");
        stackAuthenticationEntity.withPublicKeyId(publicKeyId);
        return stackAuthenticationEntity;
    }

    @Override
    public String getDefaultClusterDefinitionName() {
        return AwsParameters.DEFAULT_CLUSTER_DEFINTION_NAME;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsArn() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        String roleArn = getTestParameter().getRequired(AwsParameters.Credential.ROLE_ARN);
        roleBasedCredentialParameters.setRoleArn(roleArn);
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidArn() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
        roleBasedCredentialParameters.setRoleArn("arn:aws:iam::123456789012:role/fake");
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        String accessKeyId = getTestParameter().getRequired(AwsParameters.Credential.ACCESS_KEY_ID);
        keyBasedCredentialParameters.setAccessKey(accessKeyId);
        String secretKey = getTestParameter().getRequired(AwsParameters.Credential.SECRET_KEY);
        keyBasedCredentialParameters.setSecretKey(secretKey);
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidAccessKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setAccessKey("ABCDEFGHIJKLMNOPQRST");
        keyBasedCredentialParameters.setSecretKey(getTestParameter().get(AwsParameters.Credential.SECRET_KEY));
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialV4Parameters awsCredentialDetailsInvalidSecretKey() {
        AwsCredentialV4Parameters parameters = new AwsCredentialV4Parameters();
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setSecretKey("123456789ABCDEFGHIJKLMNOP0123456789=ABC+");
        keyBasedCredentialParameters.setAccessKey(getTestParameter().get(AwsParameters.Credential.ACCESS_KEY_ID));
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }
}
