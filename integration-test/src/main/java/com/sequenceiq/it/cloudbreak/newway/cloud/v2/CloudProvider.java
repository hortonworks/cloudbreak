package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
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

public interface CloudProvider {

    String availabilityZone();

    String region();

    String location();

    ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog);

    ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings);

    InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template);

    VolumeV4TestDto attachedVolume(VolumeV4TestDto volume);

    NetworkV2TestDto network(NetworkV2TestDto network);

    StackTestDtoBase stack(StackTestDtoBase stack);

    ClusterTestDto cluster(ClusterTestDto cluster);

    String getSubnetCIDR();

    CloudPlatform getCloudPlatform();

    CredentialTestDto credential(CredentialTestDto credential);

    EnvironmentTestDto environment(EnvironmentTestDto environment);

    PlacementSettingsTestDto placement(PlacementSettingsTestDto placement);

    StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity);

    Integer gatewayPort(StackTestDtoBase stackEntity);

    String getBlueprintName();

    StackV4ParameterBase stackParameters();
}
