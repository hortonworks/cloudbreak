package com.sequenceiq.it.cloudbreak.newway.cloud.v2.yarn;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

@Component
public class YarnCloudProvider extends AbstractCloudProvider {

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        return credential
                .withDescription(CommonCloudParameters.CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(CloudPlatform.YARN.name())
                .withYarnParameters(yarnCredentialParameters());
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return environment
                .withLocation(YarnParameters.DEFAULT_LOCATION);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return placement.withRegion(region());
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault(YarnParameters.REGION, YarnParameters.DEFAULT_REGION);
    }

    public String location() {
        return getTestParameter().getWithDefault(YarnParameters.LOCATION, YarnParameters.DEFAULT_LOCATION);
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
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return cluster
                .withValidateClusterDefinition(Boolean.FALSE)
                .withClusterDefinitionName(getClusterDefinitionName());
    }

    @Override
    public YarnStackV4Parameters stackParameters() {
        YarnStackV4Parameters yarnStackV4Parameters = new YarnStackV4Parameters();
        yarnStackV4Parameters.setYarnQueue(getQueue());
        return yarnStackV4Parameters;
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return volume.withSize(Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.VOLUME_SIZE, YarnParameters.DEFAULT_VOLUME_SIZE)))
                .withCount(Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.VOLUME_COUNT, YarnParameters.DEFAULT_VOLUME_COUNT)));
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        return network.withYarn(networkParameters()).withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        imageCatalog.withUrl(getTestParameter().getWithDefault(YarnParameters.Image.CATALOG_URL, YarnParameters.DEFAULT_IMAGE_CATALOG_URL));
        return imageCatalog;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings.withImageId(getTestParameter().getWithDefault(YarnParameters.Image.ID, YarnParameters.DEFAULT_IMAGE_ID))
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsTestDto.class).getName());
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(YarnParameters.AVAILABILITY_ZONE, null);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getClusterDefinitionName() {
        return getTestParameter().getWithDefault(CommonCloudParameters.CLUSTER_DEFINITION_NAME, YarnParameters.DEFAULT_CLUSTER_DEFINTION_NAME);
    }

    public String getQueue() {
        return getTestParameter().getWithDefault(YarnParameters.YARN_QUEUE, YarnParameters.DEFAULT_QUEUE);
    }

    public Integer getCPUCount() {
        return Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.CPU_COUNT, YarnParameters.DEFAULT_CPU_COUNT));
    }

    public Integer getMemorySize() {
        return Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.MEMORY_SIZE, YarnParameters.DEFAULT_MEMORY_SIZE));
    }

    public YarnCredentialV4Parameters yarnCredentialParameters() {
        YarnCredentialV4Parameters yarnCredentialV4Parameters = new YarnCredentialV4Parameters();
        yarnCredentialV4Parameters.setEndpoint(getTestParameter().getWithDefault(YarnParameters.Credential.ENDPOINT, YarnParameters.DEFAULT_ENDPOINT));
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