package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

public interface CloudProvider {

    String availabilityZone();

    String region();

    String location();

    ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog);

    InstanceTemplateV4Entity template(InstanceTemplateV4Entity template);

    VolumeV4Entity attachedVolume(VolumeV4Entity volume);

    NetworkV2Entity network(NetworkV2Entity network);

    StackV4EntityBase stack(StackV4EntityBase stack);

    String getSubnetCIDR();

    CloudPlatform getCloudPlatform();

    CredentialTestDto credential(CredentialTestDto credential);

    EnvironmentEntity environment(EnvironmentEntity environment);

    PlacementSettingsEntity placement(PlacementSettingsEntity placement);

    StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity);

    Integer gatewayPort(StackV4EntityBase stackEntity);

    String getDefaultClusterDefinitionName();
}
