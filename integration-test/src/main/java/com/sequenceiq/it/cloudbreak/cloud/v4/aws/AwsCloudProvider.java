package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;

@Component
public class AwsCloudProvider extends AbstractCloudProvider {

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String KEY_BASED_CREDENTIAL = "key";

    @Inject
    private AwsProperties awsProperties;

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(awsProperties.getInstance().getType());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return template.withInstanceType(awsProperties.getInstance().getType());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withAws(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox.withAws(distroXParameters());
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getBlueprintName());
    }

    @Override
    public AwsStackV4Parameters stackParameters() {
        return new AwsStackV4Parameters();
    }

    public AwsDistroXV1Parameters distroXParameters() {
        return new AwsDistroXV1Parameters();
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
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
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

    public AwsNetworkV4Parameters networkParameters() {
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setVpcId(getVpcId());
        awsNetworkV4Parameters.setSubnetId(getSubnetId());
        return awsNetworkV4Parameters;
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network.withAws(distroXNetworkParameters());
    }

    private AwsNetworkV1Parameters distroXNetworkParameters() {
        AwsNetworkV1Parameters params = new AwsNetworkV1Parameters();
        params.setSubnetId(getSubnetId());
        return params;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withSubnetIDs(getSubnetIDs())
                .withAws(environmentNetworkParameters());
    }

    private EnvironmentNetworkAwsParams environmentNetworkParameters() {
        EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkAwsParams.setVpcId(getVpcId());
        return environmentNetworkAwsParams;
    }

    public Set<String> getSubnetIDs() {
        return awsProperties.getSubnetIds();
    }

    public String getVpcId() {
        return awsProperties.getVpcId();
    }

    public String getSubnetId() {
        Set<String> subnetIDs = awsProperties.getSubnetIds();
        return subnetIDs.iterator().next();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        String credentialType = awsProperties.getCredential().getType();
        AwsCredentialParameters parameters;
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

    public AwsCredentialParameters awsCredentialDetailsArn() {
        AwsCredentialParameters parameters = new AwsCredentialParameters();
        RoleBasedParameters roleBasedCredentialParameters = new RoleBasedParameters();
        String roleArn = awsProperties.getCredential().getRoleArn();
        roleBasedCredentialParameters.setRoleArn(roleArn);
        parameters.setRoleBased(roleBasedCredentialParameters);
        return parameters;
    }

    public AwsCredentialParameters awsCredentialDetailsKey() {
        AwsCredentialParameters parameters = new AwsCredentialParameters();
        KeyBasedParameters keyBasedCredentialParameters = new KeyBasedParameters();
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        keyBasedCredentialParameters.setAccessKey(accessKeyId);
        String secretKey = awsProperties.getCredential().getSecretKey();
        keyBasedCredentialParameters.setSecretKey(secretKey);
        parameters.setKeyBased(keyBasedCredentialParameters);
        return parameters;
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage
                .withFileSystemType(getFileSystemType())
                .withBaseLocation(getBaseLocation())
                .withS3(s3CloudStorageParameters());
    }

    public S3CloudStorageV1Parameters s3CloudStorageParameters() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(awsProperties.getCloudstorage().getS3().getInstanceProfile());
        return s3CloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        return s3CloudStorageV1Parameters.getType();
    }

    @Override
    public String getBaseLocation() {
        return String.join("/", awsProperties.getCloudstorage().getBaseLocation(), DEFAULT_STORAGE_NAME);
    }
}