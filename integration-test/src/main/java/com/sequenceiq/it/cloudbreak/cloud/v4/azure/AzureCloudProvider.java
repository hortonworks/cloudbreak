package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.AzureNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.RootVolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;

@Component
public class AzureCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

    @Inject
    private AzureProperties azureProperties;

    @Inject
    private AzureCloudFunctionality azureCloudFunctionality;

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        AzureCredentialRequestParameters parameters = new AzureCredentialRequestParameters();
        parameters.setSubscriptionId(azureProperties.getCredential().getSubscriptionId());
        parameters.setTenantId(azureProperties.getCredential().getTenantId());
        AppBasedRequest appBased = new AppBasedRequest();
        appBased.setAccessKey(azureProperties.getCredential().getAppId());
        appBased.setSecretKey(azureProperties.getCredential().getAppPassword());
        parameters.setAppBased(appBased);
        validateCredential(parameters);
        return credential.withAzureParameters(parameters)
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withDescription(commonCloudProperties().getDefaultCredentialDescription());
    }

    private void validateCredential(AzureCredentialRequestParameters parameters) {
        if (StringUtils.isEmpty(parameters.getSubscriptionId()) && StringUtils.isEmpty(parameters.getTenantId())
                && StringUtils.isEmpty(parameters.getAppBased().getAccessKey()) && StringUtils.isEmpty(parameters.getAppBased().getSecretKey())) {
            throw new TestFailException("Invalid azure credentials. You should define: \n" +
                    "\t * subscription with tenant: `integrationtest.azure.credential.subscriptionId` and `integrationtest.azure.credential.tenantId` and\n" +
                    "\t * app based: `integrationtest.azure.credential.appId` and `integrationtest.azure.credential.appPassword`");
        }
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withAzure(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox.withAzure(distroXParameters());
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
    public AzureStackV4Parameters stackParameters() {
        return new AzureStackV4Parameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return azureCloudFunctionality;
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
    }

    @Override
    public String getStorageOptimizedInstanceType() {
        return azureProperties.getStorageOptimizedInstance().getType();
    }

    public AzureDistroXV1Parameters distroXParameters() {
        return new AzureDistroXV1Parameters();
    }

    @Override
    public String region() {
        return azureProperties.getRegion();
    }

    @Override
    public String location() {
        return azureProperties.getLocation();
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(azureProperties.getInstance().getType());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return template.withInstanceType(azureProperties.getInstance().getType());
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = azureProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = azureProperties.getInstance().getVolumeCount();
        String attachedVolumeType = azureProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        int attachedVolumeSize = azureProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = azureProperties.getInstance().getVolumeCount();
        String attachedVolumeType = azureProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        int rootVolumeSize = azureProperties.getInstance().getRootVolumeSize();
        return rootVolume.withSize(rootVolumeSize);
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        int rootVolumeSize = azureProperties.getInstance().getRootVolumeSize();
        return distroXRootVolume.withSize(rootVolumeSize);
    }

    @Override
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        LoggingRequest loggingRequest = new LoggingRequest();
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setManagedIdentity(getLoggerIdentity());
        adlsGen2CloudStorageV1Parameters.setSecure(getSecure());
        loggingRequest.setAdlsGen2(adlsGen2CloudStorageV1Parameters);
        loggingRequest.setStorageLocation(getBaseLocation());
        return loggingRequest;
    }

    @Override
    public NetworkRequest networkRequest(FreeIpaTestDto dto) {
        NetworkRequest networkRequest = new NetworkRequest();
        AzureNetworkParameters networkParameters = new AzureNetworkParameters();
        networkParameters.setSubnetId(getSubnetId());
        networkParameters.setNetworkId(getNetworkId());
        networkParameters.setNoPublicIp(getNoPublicIp());
        networkParameters.setResourceGroupName(getResourceGroupName());
        networkRequest.setAzure(networkParameters);
        return networkRequest;
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        AzureNetworkV4Parameters parameters = new AzureNetworkV4Parameters();
        parameters.setNoPublicIp(false);
        parameters.setSubnetId(azureProperties.getNetwork().getSubnetIds().stream().findFirst().get());
        parameters.setNetworkId(azureProperties.getNetwork().getNetworkId());
        parameters.setResourceGroupName(azureProperties.getNetwork().getResourceGroupName());
        return network.withAzure(parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public ServiceEndpointCreation serviceEndpoint() {
        ServiceEndpointCreation serviceEndpointCreation =
                ResourceGroupTest.isSingleResourceGroup(getTestParameter().get(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE))
                ? ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT
                : ServiceEndpointCreation.DISABLED;
        LOGGER.debug("Azure service endpoint creation: {}", serviceEndpointCreation);
        return serviceEndpointCreation;
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        AzureNetworkV1Parameters parameters = new AzureNetworkV1Parameters();
        return network.withAzure(parameters);
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withSubnetIDs(getSubnetIds())
                .withAzure(environmentNetworkParameters());
    }

    private EnvironmentNetworkAzureParams environmentNetworkParameters() {
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setNetworkId(getNetworkId());
        environmentNetworkAzureParams.setNoPublicIp(getNoPublicIp());
        environmentNetworkAzureParams.setResourceGroupName(getResourceGroupName());
        return environmentNetworkAzureParams;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public String availabilityZone() {
        return azureProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    public String getNetworkId() {
        return azureProperties.getNetwork().getNetworkId();
    }

    public Set<String> getSubnetIds() {
        return azureProperties.getNetwork().getSubnetIds();
    }

    public String getSubnetId() {
        Set<String> subnetIDs = getSubnetIds();
        return subnetIDs.iterator().next();
    }

    public Boolean getNoPublicIp() {
        return azureProperties.getNetwork().getNoPublicIp();
    }

    public String getResourceGroupName() {
        return azureProperties.getNetwork().getResourceGroupName();
    }

    @Override
    public String getBlueprintName() {
        return commonClusterManagerProperties().getInternalDistroXBlueprintName();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return commonClusterManagerProperties().getRuntimeVersion();
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public EnvironmentTestDto withResourceGroup(EnvironmentTestDto environmentTestDto, String resourceGroupUsageString, String resourceGroupName) {
        ResourceGroupUsage resourceGroupUsage = ResourceGroupUsage.valueOf(resourceGroupUsageString);

        return environmentTestDto.withAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(resourceGroupUsage)
                        .withName(resourceGroupName)
                        .build())
                .build());
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage
                .withFileSystemType(getFileSystemType())
                .withBaseLocation(getBaseLocation())
                .withAdlsGen2(adlsGen2CloudStorageParameters());
    }

    public AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters() {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setSecure(getSecure());
        adlsGen2CloudStorageV1Parameters.setManagedIdentity(getAssumerIdentity());
        return adlsGen2CloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        AdlsCloudStorageV1Parameters adlsCloudStorageV1Parameters = new AdlsCloudStorageV1Parameters();
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        WasbCloudStorageV1Parameters wasbCloudStorageV1Parameters = new WasbCloudStorageV1Parameters();
        FileSystemType fileSystemType;

        switch (azureProperties.getCloudStorage().getFileSystemType()) {
            case "WASB_INTEGRATED":
            case "WASB":
                fileSystemType = wasbCloudStorageV1Parameters.getType();
                break;
            case "ADLS":
                fileSystemType = adlsCloudStorageV1Parameters.getType();
                break;
            case "ADLS_GEN_2":
                fileSystemType = adlsGen2CloudStorageV1Parameters.getType();
                break;
            default:
                LOGGER.warn("The given {} File System Type is not in the list of Azure file system types. So we use the default one; {}",
                        azureProperties.getCloudStorage().getFileSystemType(), "ADLS_GEN_2");
                fileSystemType = adlsGen2CloudStorageV1Parameters.getType();
                break;
        }

        return fileSystemType;
    }

    @Override
    public String getBaseLocation() {
        return azureProperties.getCloudStorage().getBaseLocation();
    }

    public String getAssumerIdentity() {
        return azureProperties.getCloudStorage().getAdlsGen2().getAssumerIdentity();
    }

    public String getLoggerIdentity() {
        return azureProperties.getCloudStorage().getAdlsGen2().getLoggerIdentity();
    }

    public Boolean getSecure() {
        return azureProperties.getCloudStorage().getSecure();
    }

    public String getAccountName() {
        return azureProperties.getCloudStorage().getAccountName();
    }

    public String getAccountKey() {
        return azureProperties.getCloudStorage().getAccountKey();
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings
                .withImageId(azureProperties.getBaseimage().getImageId())
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (azureProperties.getBaseimage().getImageId() == null || azureProperties.getBaseimage().getImageId().isEmpty()) {
            try {
                List<ImageV4Response> images = cloudbreakClient
                        .getDefaultClient()
                        .imageCatalogV4Endpoint()
                        .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                                CloudPlatform.AZURE.name(), null, null).getCdhImages();

                ImageV4Response olderImage = images.get(images.size() - 2);
                Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
                Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
                Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | Description: %s | Stack Version: %s ", olderImage.getDate(),
                        olderImage.getUuid(), olderImage.getStackDetails().getVersion(), olderImage.getDescription()));
                azureProperties.getBaseimage().setImageId(olderImage.getUuid());

                return olderImage.getUuid();
            } catch (Exception e) {
                LOGGER.error("Cannot fetch pre-warmed images of {} image catalog!", imageCatalogTestDto.getRequest().getName());
                throw new TestFailException(" Cannot fetch pre-warmed images of " + imageCatalogTestDto.getRequest().getName() + " image catalog!", e);
            }
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", azureProperties.getBaseimage().getImageId()));
            return azureProperties.getBaseimage().getImageId();
        }
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (azureProperties.getBaseimage().getImageId() == null || azureProperties.getBaseimage().getImageId().isEmpty()) {
            String imageId = getLatestBaseImage(imageCatalogTestDto, cloudbreakClient, CloudPlatform.AZURE.name());
            azureProperties.getBaseimage().setImageId(imageId);
            return imageId;
        } else {
            return getLatestBaseImageID();
        }
    }

    @Override
    public String getLatestBaseImageID() {
        Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
        Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
        Log.log(LOGGER, format(" Image ID for SDX create: %s ", azureProperties.getBaseimage().getImageId()));
        return azureProperties.getBaseimage().getImageId();
    }

    @Override
    public void setImageId(String id) {
        azureProperties.getBaseimage().setImageId(id);
    }

    private String notImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return azureProperties.getFreeipa().getUpgrade().getImageId();
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return azureProperties.getFreeipa().getUpgrade().getCatalog();
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto.withAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyResourceGroupName(getEncryptionResourceGroupName())
                        .withEncryptionKeyUrl(getEncryptionKeyUrl())
                        .build())
                .build());
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    public String getEncryptionResourceGroupName() {
        return azureProperties.getDiskEncryption().getResourceGroupName();
    }

    public String getEncryptionKeyUrl() {
        return azureProperties.getDiskEncryption().getEncryptionKeyUrl();
    }
}
