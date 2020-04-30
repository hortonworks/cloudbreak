package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public interface CloudProvider {

    String availabilityZone();

    String region();

    String location();

    ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog);

    ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings);

    DistroXImageTestDto imageSettings(DistroXImageTestDto imageSettings);

    String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient);

    String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient);

    InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template);

    DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template);

    VolumeV4TestDto attachedVolume(VolumeV4TestDto volume);

    DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume);

    NetworkV4TestDto network(NetworkV4TestDto network);

    DistroXNetworkTestDto network(DistroXNetworkTestDto network);

    EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network);

    TelemetryTestDto telemetry(TelemetryTestDto telemetry);

    StackTestDtoBase stack(StackTestDtoBase stack);

    ClusterTestDto cluster(ClusterTestDto cluster);

    DistroXTestDtoBase distrox(DistroXTestDtoBase distrox);

    DistroXClusterTestDto cluster(DistroXClusterTestDto cluster);

    SdxTestDto sdx(SdxTestDto sdx);

    SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal);

    SdxRepairTestDto sdxRepair(SdxRepairTestDto sdxRepair);

    SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage);

    FileSystemType getFileSystemType();

    String getBaseLocation();

    String getSubnetCIDR();

    String getAccessCIDR();

    Map<String, String> getTags();

    SdxClusterShape getClusterShape();

    SdxClusterShape getInternalClusterShape();

    CloudPlatform getCloudPlatform();

    CredentialTestDto credential(CredentialTestDto credential);

    EnvironmentTestDto environment(EnvironmentTestDto environment);

    PlacementSettingsTestDto placement(PlacementSettingsTestDto placement);

    StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity);

    EnvironmentAuthenticationTestDto environmentAuthentication(EnvironmentAuthenticationTestDto environmentAuthenticationEntity);

    EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto);

    Integer gatewayPort(StackTestDtoBase stackEntity);

    Integer gatewayPort(FreeIPATestDto stackEntity);

    String getBlueprintName();

    String getBlueprintCdhVersion();

    StackV4ParameterBase stackParameters();

    CloudFunctionality getCloudFunctionality();

    void setImageCatalogName(String name);

    String getImageCatalogName();

    void setImageCatalogUrl(String url);

    void setImageId(String id);

    void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request);

    AttachedFreeIpaRequest getAttachedFreeIpaRequest();

}
