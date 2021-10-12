package com.sequenceiq.it.cloudbreak.cloud.v4.yarn;

import static java.lang.String.format;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.YarnInstanceTemplateV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
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
import com.sequenceiq.it.cloudbreak.log.Log;
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
    public CredentialTestDto credential(CredentialTestDto credential) {
        return credential
                .withDescription(commonCloudProperties().getDefaultCredentialDescription())
                .withCloudPlatform(CloudPlatform.YARN.name())
                .withYarnParameters(yarnCredentialParameters());
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        final AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
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
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withYarn(instanceParameters());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
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
                .withBlueprintName(getBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getBlueprintName());
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
    public void setImageId(String id) {
        yarnProperties.getBaseimage().setImageId(id);
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
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
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    private EnvironmentNetworkYarnParams environmentNetworkParameters() {
        EnvironmentNetworkYarnParams environmentNetworkYarnParams = new EnvironmentNetworkYarnParams();
        environmentNetworkYarnParams.setQueue(getQueue());
        return environmentNetworkYarnParams;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings.withImageId(yarnProperties.getBaseimage().getImageId())
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return throwNotImplementedException();
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (yarnProperties.getBaseimage().getImageId() == null || yarnProperties.getBaseimage().getImageId().isEmpty()) {
            String imageId = getLatestBaseImage(imageCatalogTestDto, cloudbreakClient, CloudPlatform.YARN.name());
            yarnProperties.getBaseimage().setImageId(imageId);
            return imageId;
        } else {
            return getLatestBaseImageID();
        }
    }

    @Override
    public String getLatestBaseImageID() {
        Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
        Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
        Log.log(LOGGER, format(" Image ID for SDX create: %s ", yarnProperties.getBaseimage().getImageId()));
        return yarnProperties.getBaseimage().getImageId();
    }

    private <T> T throwNotImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String availabilityZone() {
        return yarnProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
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

    public String getInstanceProfile() {
        return null;
    }
}