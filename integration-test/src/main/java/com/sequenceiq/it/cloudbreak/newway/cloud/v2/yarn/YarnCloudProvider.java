package com.sequenceiq.it.cloudbreak.newway.cloud.v2.yarn;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.imagecatalog.ImageCatalogTestDto;

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
    public PlacementSettingsEntity placement(PlacementSettingsEntity placement) {
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
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return template.withYarn(instanceParameters());
    }

    @Override
    public StackV4EntityBase stack(StackV4EntityBase stack) {
        return stack.withYarn(stackParameters());
    }

    @Override
    public ClusterEntity cluster(ClusterEntity cluster) {
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
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        return volume.withSize(Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.VOLUME_SIZE, YarnParameters.DEFAULT_VOLUME_SIZE)))
                .withCount(Integer.parseInt(getTestParameter().getWithDefault(YarnParameters.Instance.VOLUME_COUNT, YarnParameters.DEFAULT_VOLUME_COUNT)));
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
        return network.withYarn(networkParameters()).withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        imageCatalog.withUrl(getTestParameter().getWithDefault(YarnParameters.Image.CATALOG_URL, YarnParameters.DEFAULT_IMAGE_CATALOG_URL));
        return imageCatalog;
    }

    @Override
    public ImageSettingsEntity imageSettings(ImageSettingsEntity imageSettings) {
        return imageSettings.withImageId(getTestParameter().getWithDefault(YarnParameters.Image.ID, YarnParameters.DEFAULT_IMAGE_ID))
                .withImageCatalog(imageSettings.getTestContext().given(ImageSettingsEntity.class).getName());
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(YarnParameters.AVAILABILITY_ZONE, null);
    }

    @Override
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
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
        yarnCredentialV4Parameters.setAmbariUser(getTestParameter().getWithDefault(YarnParameters.Credential.AMBARI_USER, YarnParameters.DEFAULT_AMBARI_USER));
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