package com.sequenceiq.it.cloudbreak.cloud.v4.aws;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;

@Component
public class AwsCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

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
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        AwsInstanceTemplateV1Parameters awsParameters = new AwsInstanceTemplateV1Parameters();
        awsParameters.setSpot(getAwsInstanceTemplateV1SpotParameters());
        return template.withInstanceType(awsProperties.getInstance().getType())
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
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public EnvironmentTestDto setS3Guard(EnvironmentTestDto environmentTestDto, String tableName) {
        AwsEnvironmentParameters awsEnvironmentParameters = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName(tableName);
        awsEnvironmentParameters.setS3guard(s3GuardRequestParameters);
        return environmentTestDto.withAws(awsEnvironmentParameters);
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
        return commonClusterManagerProperties().getInternalDistroXBlueprintName();
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
        return String.join("/", awsProperties.getCloudStorage().getBaseLocation(), DEFAULT_STORAGE_NAME);
    }

    public String getInstanceProfile() {
        return awsProperties.getCloudStorage().getS3().getInstanceProfile();
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings
                .withImageId(awsProperties.getBaseimage().getImageId())
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (awsProperties.getBaseimage().getImageId() == null || awsProperties.getBaseimage().getImageId().isEmpty()) {
            try {
                List<ImageV4Response> images = cloudbreakClient
                        .getDefaultClient()
                        .imageCatalogV4Endpoint()
                        .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                                CloudPlatform.AWS.name(), null, null).getCdhImages();

                ImageV4Response olderImage = images.get(images.size() - 2);
                Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
                Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
                Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | Description: %s | Stack Version: %s ", olderImage.getDate(),
                        olderImage.getUuid(), olderImage.getStackDetails().getVersion(), olderImage.getDescription()));
                awsProperties.getBaseimage().setImageId(olderImage.getUuid());

                return olderImage.getUuid();
            } catch (Exception e) {
                LOGGER.error("Cannot fetch pre-warmed images of {} image catalog!", imageCatalogTestDto.getRequest().getName());
                throw new TestFailException(" Cannot fetch pre-warmed images of " + imageCatalogTestDto.getRequest().getName() + " image catalog!", e);
            }
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", awsProperties.getBaseimage().getImageId()));
            return awsProperties.getBaseimage().getImageId();
        }
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (awsProperties.getBaseimage().getImageId() == null || awsProperties.getBaseimage().getImageId().isEmpty()) {
            String imageId = getLatestBaseImage(imageCatalogTestDto, cloudbreakClient, CloudPlatform.AWS.name());
            awsProperties.getBaseimage().setImageId(imageId);
            return imageId;
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", awsProperties.getBaseimage().getImageId()));
            return awsProperties.getBaseimage().getImageId();
        }
    }

    public String getImageCatalogUrl() {
        return commonCloudProperties().getImageCatalogUrl();
    }

    @Override
    public void setImageId(String id) {
        awsProperties.getBaseimage().setImageId(id);
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

    private AttachedFreeIpaRequest getAttachedFreeIpaRequest() {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        AwsFreeIpaParameters awsFreeIpaParameters = new AwsFreeIpaParameters();
        AwsFreeIpaSpotParameters awsFreeIpaSpotParameters = new AwsFreeIpaSpotParameters();
        awsFreeIpaSpotParameters.setPercentage(getSpotPercentage());
        awsFreeIpaParameters.setSpot(awsFreeIpaSpotParameters);
        attachedFreeIpaRequest.setAws(awsFreeIpaParameters);
        return attachedFreeIpaRequest;
    }

    private int getSpotPercentage() {
        return spotUtil.isUseSpotInstances() ? 100 : 0;
    }
}