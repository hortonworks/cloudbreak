package com.sequenceiq.it.cloudbreak.cloud.v4.yarn;

import static java.lang.String.format;

import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testng.util.Strings;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.YarnInstanceTemplateV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.RootVolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
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
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class YarnCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnCloudProvider.class);

    @Inject
    private YarnProperties yarnProperties;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

    @Override
    public VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(yarnProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(yarnProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(yarnProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(yarnProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(yarnProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(yarnProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public boolean verticalScalingSupported() {
        return yarnProperties.getVerticalScale().isSupported();
    }

    @Override
    public String getFreeIpaRebuildFullBackup() {
        throw new NotImplementedException(format("Not implemented on %s. Do you want to use against a real provider? You should set the " +
                "`integrationtest.cloudProvider` property, Values: AZURE, AWS", getCloudPlatform()));
    }

    @Override
    public String getFreeIpaRebuildDataBackup() {
        throw new NotImplementedException(format("Not implemented on %s. Do you want to use against a real provider? You should set the " +
                "`integrationtest.cloudProvider` property, Values: AZURE, AWS", getCloudPlatform()));
    }

    @Override
    public String getFreeIpaInstanceType() {
        throw new NotImplementedException(format("Not implemented on %s. Do you want to use against a real provider? You should set the " +
                "`integrationtest.cloudProvider` property, Values: AZURE, AWS", getCloudPlatform()));
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        return credential
                .withDescription(commonCloudProperties().getDefaultCredentialDescription())
                .withCloudPlatform(CloudPlatform.YARN.name())
                .withYarnParameters(yarnCredentialParameters());
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        attachedFreeIpaRequest.setCreate(Boolean.FALSE);

        return environment
                .withLocation(location())
                .withFreeIpa(attachedFreeIpaRequest);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return placement.withRegion(region());
    }

    @Override
    public String region() {
        return yarnProperties.getRegion();
    }

    public String location() {
        return yarnProperties.getLocation();
    }

    @Override
    public String getDefaultInstanceType(Architecture architecture) {
        throw new NotImplementedException("Default instance type is not configured for Yarn.");
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withYarn(instanceParameters());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture) {
        if (architecture != Architecture.X86_64) {
            throw new NotImplementedException(String.format("Architecture %s is not implemented", architecture.getName()));
        }
        return template.withYarn(distroxInstanceParameters());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withYarn(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox;
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.FALSE)
                .withBlueprintName(commonClusterManagerProperties().getInternalSdxBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(commonClusterManagerProperties().getInternalSdxBlueprintName());
    }

    @Override
    public YarnStackV4Parameters stackParameters() {
        YarnStackV4Parameters yarnStackV4Parameters = new YarnStackV4Parameters();
        yarnStackV4Parameters.setYarnQueue(getQueue());
        return yarnStackV4Parameters;
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return throwNotImplementedException();
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
    }

    @Override
    public String getStorageOptimizedInstanceType() {
        return null;
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return volume.withSize(yarnProperties.getInstance().getVolumeSize())
                .withCount(yarnProperties.getInstance().getVolumeCount());
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        return volume.withSize(yarnProperties.getInstance().getVolumeSize())
                .withCount(yarnProperties.getInstance().getVolumeCount());
    }

    @Override
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        int rootVolumeSize = yarnProperties.getInstance().getRootVolumeSize();
        return rootVolume.withSize(rootVolumeSize);
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        int rootVolumeSize = yarnProperties.getInstance().getRootVolumeSize();
        return distroXRootVolume.withSize(rootVolumeSize);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return network.withYarn(networkParameters());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withYarn(environmentNetworkParameters());
    }

    @Override
    public EnvironmentNetworkTestDto trustSetupNetwork(EnvironmentNetworkTestDto network) {
        return network(network);
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    private EnvironmentNetworkYarnParams environmentNetworkParameters() {
        EnvironmentNetworkYarnParams environmentNetworkYarnParams = new EnvironmentNetworkYarnParams();
        environmentNetworkYarnParams.setQueue(getQueue());
        return environmentNetworkYarnParams;
    }

    @Override
    public String getImageCatalogName() {
        return "yarn-image-catalog";
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return imageCatalog.withName(getImageCatalogName())
                .withUrl(yarnProperties.getBaseimage().getImageCatalogUrl())
                .withoutCleanup();
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        if (Strings.isNullOrEmpty(imageSettings.getRequest().getId())) {
            imageSettings.withImageId(yarnProperties.getBaseimage().getImageId())
                    .withOs(getOsType().getOs())
                    .withImageCatalog(getImageCatalogName());
        }
        return imageSettings;
    }

    @Override
    public DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings) {
        if (Strings.isNullOrEmpty(imageSettings.getRequest().getId())) {
            imageSettings.withImageId(yarnProperties.getBaseimage().getImageId())
                    .withOs(getOsType().getOs())
                    .withImageCatalog(getImageCatalogName());
        }
        return imageSettings;
    }

    @Override
    public String getLatestPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        // At cloudbreak-default (https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-cb-image-catalog.jsonDelete) catalog we have only
        // Base Image for YCloud provider.
        return throwNotImplementedException();
    }

    private <T> T throwNotImplementedException() {
        throw new NotImplementedException(format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String availabilityZone() {
        return yarnProperties.getAvailabilityZone();
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

    @Override
    public String getBlueprintCdhVersion() {
        return commonClusterManagerProperties().getRuntimeVersion();
    }

    public String getQueue() {
        return yarnProperties.getQueue();
    }

    public Integer getCPUCount() {
        return yarnProperties.getInstance().getCpuCount();
    }

    public Integer getMemorySize() {
        return yarnProperties.getInstance().getMemory();
    }

    public YarnParameters yarnCredentialParameters() {
        YarnParameters yarnCredentialV4Parameters = new YarnParameters();
        yarnCredentialV4Parameters.setEndpoint(yarnProperties.getCredential().getEndpoint());
        return yarnCredentialV4Parameters;
    }

    private YarnInstanceTemplateV4Parameters instanceParameters() {
        YarnInstanceTemplateV4Parameters yarnInstanceTemplateV4Parameters = new YarnInstanceTemplateV4Parameters();
        yarnInstanceTemplateV4Parameters.setCpus(getCPUCount());
        yarnInstanceTemplateV4Parameters.setMemory(getMemorySize());
        return yarnInstanceTemplateV4Parameters;
    }

    private YarnNetworkV4Parameters networkParameters() {
        YarnNetworkV4Parameters yarnNetworkV4Parameters = new YarnNetworkV4Parameters();
        yarnNetworkV4Parameters.getCloudPlatform();
        return yarnNetworkV4Parameters;
    }

    private YarnInstanceTemplateV1Parameters distroxInstanceParameters() {
        YarnInstanceTemplateV1Parameters yarnInstanceTemplateV4Parameters = new YarnInstanceTemplateV1Parameters();
        yarnInstanceTemplateV4Parameters.setCpus(getCPUCount());
        yarnInstanceTemplateV4Parameters.setMemory(getMemorySize());
        return yarnInstanceTemplateV4Parameters;
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage;
    }

    @Override
    public FileSystemType getFileSystemType() {
        return null;
    }

    @Override
    public String getBaseLocation() {
        return null;
    }

    @Override
    public String getBaseLocationForPreTermination() {
        return null;
    }

    public String getInstanceProfile() {
        return null;
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withDatabaseEncryptionKey(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public EnvironmentTestDto withResourceEncryptionUserManagedIdentity(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    public OsType getOsType() {
        return Objects.requireNonNullElse(yarnProperties.getBaseimage().getOsType(), OsType.RHEL8);
    }
}
