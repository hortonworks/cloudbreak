package com.sequenceiq.it.cloudbreak.newway.cloud.v2.yarn;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

@Component
public class YarnCloudProvider extends AbstractCloudProvider {

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
        return environment
                .withLocation(location());
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
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withYarn(stackParameters());
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.FALSE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    public YarnStackV4Parameters stackParameters() {
        YarnStackV4Parameters yarnStackV4Parameters = new YarnStackV4Parameters();
        yarnStackV4Parameters.setYarnQueue(getQueue());
        return yarnStackV4Parameters;
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return volume.withSize(yarnProperties.getInstance().getVolumeSize())
                .withCount(yarnProperties.getInstance().getVolumeCount());
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        return network.withYarn(networkParameters()).withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        imageCatalog.withUrl(yarnProperties.getImageCatalogUrl());
        return imageCatalog;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings.withImageId(yarnProperties.getImageId())
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName());
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
        return yarnProperties.getDefaultBlueprintName();
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

    public YarnCredentialV4Parameters yarnCredentialParameters() {
        YarnCredentialV4Parameters yarnCredentialV4Parameters = new YarnCredentialV4Parameters();
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
}