package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.RootVolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.SubnetId;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXRootVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;

@Component
public class AwsCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "apitest" + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String KEY_BASED_CREDENTIAL = "key";

    @Inject
    private AwsProperties awsProperties;

    @Inject
    private AwsCloudFunctionality awsCloudFunctionality;

    @Inject
    private SpotUtil spotUtil;

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        AwsInstanceTemplateV4Parameters aws = new AwsInstanceTemplateV4Parameters();
        AwsInstanceTemplateV4SpotParameters spot = new AwsInstanceTemplateV4SpotParameters();
        spot.setPercentage(getSpotPercentage());
        aws.setSpot(spot);
        return template.withInstanceType(awsProperties.getInstance().getType())
                .withAws(aws);
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture) {
        AwsInstanceTemplateV1Parameters awsParameters = new AwsInstanceTemplateV1Parameters();
        awsParameters.setSpot(getAwsInstanceTemplateV1SpotParameters());
        AwsProperties.Instance instance = architecture == Architecture.X86_64 ? awsProperties.getInstance() : awsProperties.getArm64Instance();
        return template.withInstanceType(instance.getType())
                .withAws(awsParameters);
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
                .withBlueprintName(getDataEngDistroXBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getDataEngDistroXBlueprintName());
    }

    @Override
    public AwsStackV4Parameters stackParameters() {
        return new AwsStackV4Parameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return awsCloudFunctionality;
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
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        LoggingRequest loggingRequest = new LoggingRequest();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(getInstanceProfile());
        loggingRequest.setS3(s3CloudStorageV1Parameters);
        loggingRequest.setStorageLocation(getBaseLocation());
        loggingRequest.setEnabledSensitiveStorageLogs(Set.of("SALT"));
        return loggingRequest;
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
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        int rootVolumeSize = awsProperties.getInstance().getRootVolumeSize();
        return rootVolume.withSize(rootVolumeSize);
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        int rootVolumeSize = awsProperties.getInstance().getRootVolumeSize();
        return distroXRootVolume.withSize(rootVolumeSize);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return network.withAws(networkParameters());
    }

    @Override
    public ServiceEndpointCreation serviceEndpoint() {
        return ServiceEndpointCreation.ENABLED;
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

    @Override
    public EnvironmentNetworkTestDto trustSetupNetwork(EnvironmentNetworkTestDto network) {
        return network.withSubnetIDs(awsProperties.getTrust().getSubnetIds())
                .withAws(environmentNetworkParameters());
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
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
    public InstanceGroupNetworkV4Request instanceGroupNetworkV4Request(SubnetId subnetId) {
        if (isMultiAZ()) {
            InstanceGroupNetworkV4Request result = new InstanceGroupNetworkV4Request();
            result.createAws();
            result.getAws().setSubnetIds(subnetId.collectSubnets(new LinkedList<>(awsProperties.getSubnetIds())));
            return result;
        } else {
            return null;
        }
    }

    @Override
    public InstanceGroupNetworkV1Request instanceGroupNetworkV1Request(SubnetId subnetId) {
        if (isMultiAZ()) {
            InstanceGroupNetworkV1Request result = new InstanceGroupNetworkV1Request();
            result.createAws();
            result.getAws().setSubnetIds(subnetId.collectSubnets(new LinkedList<>(awsProperties.getSubnetIds())));
            return result;
        } else {
            return null;
        }
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
        if (getGovCloud()) {
            parameters.setGovCloud(true);
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
    public String getDefaultInstanceType(Architecture architecture) {
        if (Architecture.ARM64.equals(architecture)) {
            return awsProperties.getArm64Instance().getType();
        }
        return awsProperties.getInstance().getType();
    }

    @Override
    public String verticalScaleVolumeType() {
        return awsProperties.getVerticalScale().getVolumeType();
    }

    @Override
    public String availabilityZone() {
        return awsProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        StackAuthenticationV4Request request = stackAuthenticationEntity.getRequest();
        stackAuthenticationEntity.withPublicKeyId(StringUtils.isBlank(request.getPublicKeyId())
                ? awsProperties.getPublicKeyId()
                : request.getPublicKeyId());
        stackAuthenticationEntity.withPublicKey(request.getPublicKey());
        stackAuthenticationEntity.withLoginUserName(request.getLoginUserName());
        return stackAuthenticationEntity;
    }

    @Override
    public String getBlueprintCdhVersion() {
        return commonClusterManagerProperties().getRuntimeVersion();
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
        s3CloudStorageV1Parameters.setInstanceProfile(awsProperties.getCloudStorage().getS3().getInstanceProfile());
        return s3CloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        return s3CloudStorageV1Parameters.getType();
    }

    @Override
    public String getBaseLocation() {
        return String.join("/", awsProperties.getCloudStorage().getBaseLocation(), trimObjectName(DEFAULT_STORAGE_NAME),
                getSuiteName(), getTestName());
    }

    @Override
    public String getBaseLocationForPreTermination() {
        return String.join("/", awsProperties.getCloudStorage().getBaseLocation(), "pre-termination");
    }

    public String getInstanceProfile() {
        return awsProperties.getCloudStorage().getS3().getInstanceProfile();
    }

    public String getImageCatalogUrl() {
        return commonCloudProperties().getImageCatalogUrl();
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
        AwsInstanceTemplateV1Parameters awsInstanceTemplateV1Parameters = new AwsInstanceTemplateV1Parameters();
        AwsEncryptionV1Parameters awsEncryptionV1Parameters = new AwsEncryptionV1Parameters();
        awsEncryptionV1Parameters.setType(EncryptionType.DEFAULT);
        awsInstanceTemplateV1Parameters.setEncryption(awsEncryptionV1Parameters);
        awsInstanceTemplateV1Parameters.setSpot(getAwsInstanceTemplateV1SpotParameters());
        instanceTemplateV1Request.setAws(awsInstanceTemplateV1Parameters);
    }

    private AwsInstanceTemplateV1SpotParameters getAwsInstanceTemplateV1SpotParameters() {
        AwsInstanceTemplateV1SpotParameters awsInstanceTemplateV1SpotParameters = new AwsInstanceTemplateV1SpotParameters();
        awsInstanceTemplateV1SpotParameters.setPercentage(getSpotPercentage());
        return awsInstanceTemplateV1SpotParameters;
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return super.environment(environment)
                .withFreeIpa(getAttachedFreeIpaRequest());
    }

    @Override
    public String getVariant() {
        if (isMultiAZ()) {
            return "AWS_NATIVE";
        } else if (getGovCloud()) {
            return "AWS_NATIVE_GOV";
        } else {
            return "AWS";
        }
    }

    @Override
    public String getStorageOptimizedInstanceType() {
        return awsProperties.getStorageOptimizedInstance().getType();
    }

    @Override
    public VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(awsProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(awsProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(awsProperties.getVerticalScale().getDatahub().getGroup())
                .withInstanceType(awsProperties.getVerticalScale().getDatahub().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(awsProperties.getVerticalScale().getDatalake().getGroup())
                .withInstanceType(awsProperties.getVerticalScale().getDatalake().getInstanceType());
    }

    @Override
    public boolean verticalScalingSupported() {
        return awsProperties.getVerticalScale().isSupported();
    }

    private AttachedFreeIpaRequest getAttachedFreeIpaRequest() {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        AwsFreeIpaParameters awsFreeIpaParameters = new AwsFreeIpaParameters();
        AwsFreeIpaSpotParameters awsFreeIpaSpotParameters = new AwsFreeIpaSpotParameters();
        awsFreeIpaSpotParameters.setPercentage(getSpotPercentage());
        awsFreeIpaParameters.setSpot(awsFreeIpaSpotParameters);
        attachedFreeIpaRequest.setAws(awsFreeIpaParameters);
        attachedFreeIpaRequest.setEnableMultiAz(isMultiAZ());
        return attachedFreeIpaRequest;
    }

    private int getSpotPercentage() {
        return spotUtil.isUseSpotInstances() ? 100 : 0;
    }

    @Override
    public boolean getGovCloud() {
        return awsProperties.getGovCloud();
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return awsProperties.getFreeipa().getUpgrade().getImageId();
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return awsProperties.getFreeipa().getUpgrade().getCatalog();
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto.withAws(AwsEnvironmentParameters.builder()
                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters.builder()
                        .withEncryptionKeyArn(getEncryptionKeyArn(true))
                        .build())
                .build());
    }

    @Override
    public EnvironmentTestDto withDatabaseEncryptionKey(EnvironmentTestDto environmentTestDto) {
        return withResourceEncryption(environmentTestDto);
    }

    @Override
    public EnvironmentTestDto withResourceEncryptionUserManagedIdentity(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    public String getEncryptionKeyArn(boolean environmentEncryption) {
        return environmentEncryption
                ? awsProperties.getDiskEncryption().getEnvironmentKey()
                : awsProperties.getDiskEncryption().getDatahubKey();
    }

    @Override
    public void verifyDiskEncryptionKey(DetailedEnvironmentResponse environment, String environmentName) {
        String encryptionKeyArn = environment.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn();
        if (StringUtils.isEmpty(encryptionKeyArn)) {
            LOGGER.error(format("KMS key is not available for '%s' environment!", environmentName));
            throw new TestFailException(format("KMS key is not available for '%s' environment!", environmentName));
        } else {
            LOGGER.info(format("Environment '%s' create has been done with '%s' KMS key.", environmentName, encryptionKeyArn));
            Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' KMS key. ", environmentName, encryptionKeyArn));
        }
    }

    @Override
    public void verifyVolumeEncryptionKey(List<String> volumeKmsKeyIds, String environmentName) {
        String kmsKeyArn = getEncryptionKeyArn(true);
        if (volumeKmsKeyIds.stream().noneMatch(keyId -> keyId.equalsIgnoreCase(kmsKeyArn))) {
            LOGGER.error(format("Volume has not been encrypted with '%s' KMS key!", kmsKeyArn));
            throw new TestFailException(format("Volume has not been encrypted with '%s' KMS key!", kmsKeyArn));
        } else {
            LOGGER.info(format("Volume has been encrypted with '%s' KMS key.", kmsKeyArn));
            Log.then(LOGGER, format(" Volume has been encrypted with '%s' KMS key. ", kmsKeyArn));
        }
    }

    @Override
    public boolean isMultiAZ() {
        return awsProperties.getMultiaz();
    }

    @Override
    public boolean isExternalDatabaseSslEnforcementSupported() {
        return awsProperties.getExternalDatabaseSslEnforcementSupported();
    }

    @Override
    public String getEmbeddedDbUpgradeSourceVersion() {
        return awsProperties.getEmbeddedDbUpgradeSourceVersion();
    }

    @Override
    public String getFreeIpaRebuildFullBackup() {
        return awsProperties.getFreeipa().getRebuild().getFullbackup();
    }

    @Override
    public String getFreeIpaRebuildDataBackup() {
        return awsProperties.getFreeipa().getRebuild().getDatabackup();
    }

    @Override
    public String getFreeIpaInstanceType() {
        return awsProperties.getFreeipa().getInstanceType();
    }
}
