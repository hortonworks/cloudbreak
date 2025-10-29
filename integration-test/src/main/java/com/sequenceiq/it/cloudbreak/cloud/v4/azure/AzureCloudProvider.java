package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import static java.lang.String.format;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testng.util.Strings;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.Architecture;
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
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties.Network;
import com.sequenceiq.it.cloudbreak.config.azure.ResourceGroupProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class AzureCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "apitest" + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String MARKETPLACE_REGION = "default";

    @Inject
    private AzureProperties azureProperties;

    @Inject
    private AzureCloudFunctionality azureCloudFunctionality;

    @Inject
    private ResourceGroupProperties resourceGroupProperties;

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
    public VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(azureProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(azureProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(azureProperties.getVerticalScale().getDatahub().getGroup())
                .withInstanceType(azureProperties.getVerticalScale().getDatahub().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(azureProperties.getVerticalScale().getDatalake().getGroup())
                .withInstanceType(azureProperties.getVerticalScale().getDatalake().getInstanceType());
    }

    @Override
    public boolean verticalScalingSupported() {
        return azureProperties.getVerticalScale().isSupported();
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
                .withBlueprintName(getDataEngDistroXBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getDataEngDistroXBlueprintName());
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

    @Override
    public SdxDatabaseRequest extendDBRequestWithProviderParams(SdxDatabaseRequest sdxDatabaseRequest) {
        sdxDatabaseRequest.setSdxDatabaseAzureRequest(new SdxDatabaseAzureRequest());
        return sdxDatabaseRequest;
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
    public String getDefaultInstanceType(Architecture architecture) {
        if (Architecture.ARM64.equals(architecture)) {
            throw new NotImplementedException("Arm64 instance is not configured for Azure.");
        }
        return azureProperties.getInstance().getType();
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(azureProperties.getInstance().getType());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture) {
        if (architecture != Architecture.X86_64) {
            throw new NotImplementedException(String.format("Architecture %s is not implemented", architecture.getName()));
        }
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
        loggingRequest.setEnabledSensitiveStorageLogs(Set.of("SALT"));
        return loggingRequest;
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto networkTestDto) {
        AzureNetworkV4Parameters parameters = new AzureNetworkV4Parameters();
        Network network = azureProperties.getNetwork();
        parameters.setNoPublicIp(network.getNoPublicIp());
        parameters.setSubnetId(network.getSubnetIds().stream().findFirst().orElse(null));
        parameters.setNetworkId(network.getNetworkId());
        parameters.setResourceGroupName(network.getResourceGroupName());
        parameters.setDatabasePrivateDnsZoneId(network.getDatabasePrivateDnsZoneId());
        return networkTestDto.withAzure(parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public ServiceEndpointCreation serviceEndpoint() {
        ServiceEndpointCreation serviceEndpointCreation =
                ResourceGroupTest.isSingleResourceGroup(resourceGroupProperties.getResourceGroupUsage())
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
                .withAzure(environmentNetworkParameters())
                .withServiceEndpoints(serviceEndpoint())
                .disableAzurePrivateEndpoint();
    }

    private EnvironmentNetworkAzureParams environmentNetworkParameters() {
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        Network network = azureProperties.getNetwork();
        environmentNetworkAzureParams.setNetworkId(network.getNetworkId());
        environmentNetworkAzureParams.setNoPublicIp(network.getNoPublicIp());
        environmentNetworkAzureParams.setResourceGroupName(network.getResourceGroupName());
        environmentNetworkAzureParams.setDatabasePrivateDnsZoneId(network.getDatabasePrivateDnsZoneId());
        environmentNetworkAzureParams.setNoOutboundLoadBalancer(false);
        return environmentNetworkAzureParams;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public String verticalScaleVolumeType() {
        return azureProperties.getVerticalScale().getVolumeType();
    }

    @Override
    public String availabilityZone() {
        return azureProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        StackAuthenticationV4Request request = stackAuthenticationEntity.getRequest();
        stackAuthenticationEntity.withPublicKeyId(request.getPublicKeyId());
        stackAuthenticationEntity.withPublicKey(StringUtils.isBlank(request.getPublicKey())
                ? commonCloudProperties().getSshPublicKey()
                : request.getPublicKey());
        stackAuthenticationEntity.withLoginUserName(request.getLoginUserName());
        return stackAuthenticationEntity;
    }

    public Set<String> getSubnetIds() {
        return azureProperties.getNetwork().getSubnetIds();
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

        if (environmentTestDto.getAzure() == null) {
            environmentTestDto.setAzure(AzureEnvironmentParameters.builder().build());
        }
        AzureEnvironmentParameters azureEnvironmentParameters = environmentTestDto.getAzure();
        azureEnvironmentParameters.setResourceGroup(AzureResourceGroup.builder()
                .withResourceGroupUsage(resourceGroupUsage)
                .withName(resourceGroupName)
                .build());
        environmentTestDto.setAzure(azureEnvironmentParameters);
        return environmentTestDto;
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
        return String.join("/", azureProperties.getCloudStorage().getBaseLocation(), trimObjectName(DEFAULT_STORAGE_NAME),
                getSuiteName(), getTestName());
    }

    @Override
    public String getBaseLocationForPreTermination() {
        return String.join("/", azureProperties.getCloudStorage().getBaseLocation(),
                "pre-termination");
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
    public String getLatestMarketplacePreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient,
            String runtimeVersion) {
        try {
            Optional<ImageV4Response> prewarmedImagesForRuntime = cloudbreakClient
                    .getDefaultClient()
                    .imageCatalogV4Endpoint()
                    .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                            CloudPlatform.AZURE.name(), runtimeVersion, null, false, false, null)
                    .getCdhImages().stream()
                    .filter(image -> image.getImageSetsByProvider().entrySet()
                            .stream()
                            .anyMatch(i -> i.getValue().containsKey(MARKETPLACE_REGION)))
                    .max(Comparator.comparing(ImageV4Response::getPublished));

            ImageV4Response latestPrewarmedImage = prewarmedImagesForRuntime
                    .orElseThrow(() ->
                            new IllegalStateException(format("Cannot find pre-warmed Azure Marketplace images at Azure provider for '%s' runtime version!",
                                    runtimeVersion)));
            Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
            Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | Description: %s ", latestPrewarmedImage.getDate(),
                    latestPrewarmedImage.getUuid(), latestPrewarmedImage.getDescription()));
            return latestPrewarmedImage.getUuid();
        } catch (Exception e) {
            LOGGER.error("Cannot fetch pre-warmed images from '{}' image catalog, because of: {}", imageCatalogTestDto.getRequest().getName(), e);
            throw new TestFailException(format("Cannot fetch pre-warmed images from '%s' image catalog!", imageCatalogTestDto.getRequest().getName(), e));
        }
    }

    private String notImplementedException() {
        throw new NotImplementedException(format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return azureProperties.getFreeipa().getUpgrade().getImageId();
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageId() {
        return azureProperties.getFreeipa().getMarketplace().getUpgrade().getImageId();
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return azureProperties.getFreeipa().getUpgrade().getCatalog();
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageCatalog() {
        return azureProperties.getFreeipa().getMarketplace().getUpgrade().getCatalog();
    }

    @Override
    public String getSdxMarketplaceUpgradeImageId() {
        return azureProperties.getMarketplace().getSdx().getUpgrade().getImageId();
    }

    @Override
    public String getSdxMarketplaceUpgradeImageCatalog() {
        return azureProperties.getMarketplace().getSdx().getUpgrade().getCatalog();
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        if (environmentTestDto.getAzure() == null) {
            environmentTestDto.setAzure(AzureEnvironmentParameters.builder().build());
        }
        AzureEnvironmentParameters azureEnvironmentParameters = environmentTestDto.getAzure();

        AzureResourceEncryptionParameters.Builder builder = AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyResourceGroupName(getEncryptionResourceGroupName())
                .withEncryptionKeyUrl(getDiskEncryptionKeyUrl());
        if (!Strings.isNullOrEmpty(getEncryptionManagedIdentity())) {
            builder.withUserManagedIdentity(getEncryptionManagedIdentity());
        }
        azureEnvironmentParameters.setResourceEncryptionParameters(builder.build());
        environmentTestDto.setAzure(azureEnvironmentParameters);
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withDatabaseEncryptionKey(EnvironmentTestDto environmentTestDto) {
        if (environmentTestDto.getAzure() == null) {
            environmentTestDto.setAzure(AzureEnvironmentParameters.builder().build());
        }
        AzureEnvironmentParameters azureEnvironmentParameters = environmentTestDto.getAzure();
        AzureResourceEncryptionParameters params = AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyResourceGroupName(getEncryptionResourceGroupName())
                .withEncryptionKeyUrl(getDatabaseEncryptionKeyUrl()).build();
        azureEnvironmentParameters.setResourceEncryptionParameters(params);
        environmentTestDto.setAzure(azureEnvironmentParameters);
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withResourceEncryptionUserManagedIdentity(EnvironmentTestDto environmentTestDto) {
        if (environmentTestDto.getAzure() == null) {
            environmentTestDto.setAzure(AzureEnvironmentParameters.builder().build());
        }
        AzureEnvironmentParameters azureEnvironmentParameters = environmentTestDto.getAzure();
        if (azureEnvironmentParameters.getResourceEncryptionParameters() == null) {
            azureEnvironmentParameters.setResourceEncryptionParameters(AzureResourceEncryptionParameters.builder().build());
        }
        AzureResourceEncryptionParameters encryptionParameters = azureEnvironmentParameters.getResourceEncryptionParameters();
        encryptionParameters.setUserManagedIdentity(getEncryptionManagedIdentity());
        return environmentTestDto;
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    public String getEncryptionResourceGroupName() {
        return azureProperties.getDiskEncryption().getResourceGroupName();
    }

    public String getDatabaseEncryptionKeyUrl() {
        return azureProperties.getDatabaseEncryption().getEncryptionKeyUrl();
    }

    public String getDiskEncryptionKeyUrl() {
        return azureProperties.getDiskEncryption().getEncryptionKeyUrl();
    }

    public String getEncryptionManagedIdentity() {
        return azureProperties.getDiskEncryption().getManagedIdentity();
    }

    @Override
    public void verifyDiskEncryptionKey(DetailedEnvironmentResponse environment, String environmentName) {
        String diskEncryptionSetId = environment.getAzure().getResourceEncryptionParameters().getDiskEncryptionSetId();
        if (StringUtils.isEmpty(diskEncryptionSetId)) {
            LOGGER.error(format("DES key is not available for '%s' environment!", environmentName));
            throw new TestFailException(format("DES key is not available for '%s' environment!", environmentName));
        } else {
            LOGGER.info(format("Environment '%s' create has been done with '%s' DES key.", environmentName, diskEncryptionSetId));
            Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' DES key. ", environmentName, diskEncryptionSetId));
        }
    }

    @Override
    public void verifyVolumeEncryptionKey(List<String> volumesDesIds, String environmentName) {
        String desKeyUrl = getDiskEncryptionKeyUrl();
        if (volumesDesIds.stream().noneMatch(desId -> StringUtils.containsIgnoreCase(desId, "diskEncryptionSets/" + environmentName))) {
            LOGGER.error(format("Volume has NOT been encrypted with '%s' DES key!", desKeyUrl));
            throw new TestFailException(format("Volume has NOT been encrypted with '%s' DES key!", desKeyUrl));
        } else {
            LOGGER.info(format("Volume has been encrypted with '%s' DES key.", desKeyUrl));
            Log.then(LOGGER, format(" Volume has been encrypted with '%s' DES key. ", desKeyUrl));
        }
    }

    @Override
    public boolean isExternalDatabaseSslEnforcementSupported() {
        return azureProperties.getExternalDatabaseSslEnforcementSupported();
    }

    @Override
    public String getEmbeddedDbUpgradeSourceVersion() {
        return azureProperties.getEmbeddedDbUpgradeSourceVersion();
    }

    @Override
    public String getFreeIpaRebuildFullBackup() {
        return azureProperties.getFreeipa().getRebuild().getFullbackup();
    }

    @Override
    public String getFreeIpaRebuildDataBackup() {
        return azureProperties.getFreeipa().getRebuild().getDatabackup();
    }
}
