package com.sequenceiq.it.cloudbreak.cloud.v4.openstack;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.DomainKeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.KeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.OpenstackParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.openstack.ProjectKeystoneV3Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkOpenstackParams;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EncryptionProfileTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.openstack.OpenstackCloudFunctionality;

@Component
public class OpenstackCloudProvider extends AbstractCloudProvider {

    private static final String DEFAULT_STORAGE_NAME = "apitest" + UUID.randomUUID().toString().replace("-", "");

    @Inject
    private OpenstackProperties openstackProperties;

    @Inject
    private OpenstackCloudFunctionality openstackCloudFunctionality;

    public OpenstackProperties getOpenstackProperties() {
        return openstackProperties;
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(openstackProperties.getInstance().getType())
                .withOpenStack(new OpenStackInstanceTemplateV4Parameters());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture) {
        return template.withInstanceType(openstackProperties.getInstance().getType());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withOpenStack(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        DistroXDatabaseRequest database = new DistroXDatabaseRequest();
        database.setAvailabilityType(DistroXDatabaseAvailabilityType.ON_ROOT_VOLUME);
        return distrox.withExternalDatabase(database);
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
    public OpenStackStackV4Parameters stackParameters() {
        return new OpenStackStackV4Parameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return openstackCloudFunctionality;
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = openstackProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = openstackProperties.getInstance().getVolumeCount();
        String attachedVolumeType = openstackProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        return null;
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        int attachedVolumeSize = openstackProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = openstackProperties.getInstance().getVolumeCount();
        String attachedVolumeType = openstackProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        return rootVolume;
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        return distroXRootVolume;
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return network.withOpenStack(networkParameters());
    }

    public OpenStackNetworkV4Parameters networkParameters() {
        OpenStackNetworkV4Parameters openStackNetworkV4Parameters = new OpenStackNetworkV4Parameters();
        openStackNetworkV4Parameters.setNetworkId(openstackProperties.getNetworkId());
        openStackNetworkV4Parameters.setRouterId(openstackProperties.getRouterId());
        openStackNetworkV4Parameters.setPublicNetId(openstackProperties.getPublicNetId());
        Set<String> subnetIDs = getSubnetIDs();
        if (!CollectionUtils.isEmpty(subnetIDs)) {
            openStackNetworkV4Parameters.setSubnetId(subnetIDs.iterator().next());
        }
        return openStackNetworkV4Parameters;
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        Set<String> subnetIDs = getSubnetIDs();
        if (!CollectionUtils.isEmpty(subnetIDs)) {
            network.withSubnetIDs(subnetIDs);
        } else {
            network.withNetworkCIDR(getSubnetCIDR());
        }
        return network.withOpenstack(environmentNetworkParameters());
    }

    @Override
    public EnvironmentNetworkTestDto trustSetupNetwork(EnvironmentNetworkTestDto network) {
        return network(network);
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    public EnvironmentNetworkOpenstackParams environmentNetworkParameters() {
        return EnvironmentNetworkOpenstackParams.EnvironmentNetworkOpenstackParamsBuilder.anEnvironmentNetworkOpenstackParams()
                .withNetworkId(openstackProperties.getNetworkId())
                .withRouterId(openstackProperties.getRouterId())
                .withPublicNetId(openstackProperties.getPublicNetId())
                .build();
    }

    public Set<String> getSubnetIDs() {
        return openstackProperties.getSubnetIds();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        OpenstackParameters parameters = new OpenstackParameters();
        parameters.setEndpoint(openstackProperties.getCredential().getEndpoint());
        parameters.setFacing(openstackProperties.getCredential().getFacing());
        parameters.setUserName(openstackProperties.getCredential().getUserName());
        parameters.setPassword(openstackProperties.getCredential().getPassword());

        KeystoneV3Parameters keystoneV3 = new KeystoneV3Parameters();
        if (StringUtils.isNotBlank(openstackProperties.getCredential().getProjectName())) {
            ProjectKeystoneV3Parameters project = new ProjectKeystoneV3Parameters();
            project.setProjectName(openstackProperties.getCredential().getProjectName());
            project.setProjectDomainName(openstackProperties.getCredential().getProjectDomainName());
            project.setUserDomain(openstackProperties.getCredential().getUserDomain());
            keystoneV3.setProject(project);
        } else if (StringUtils.isNotBlank(openstackProperties.getCredential().getDomainName())) {
            DomainKeystoneV3Parameters domain = new DomainKeystoneV3Parameters();
            domain.setDomainName(openstackProperties.getCredential().getDomainName());
            domain.setUserDomain(openstackProperties.getCredential().getUserDomain());
            keystoneV3.setDomain(domain);
        }

        if (keystoneV3.getProject() != null || keystoneV3.getDomain() != null) {
            parameters.setKeystoneV3(keystoneV3);
        }

        return credential
                .withDescription(commonCloudProperties().getDefaultCredentialDescription())
                .withCloudPlatform(CloudPlatform.OPENSTACK.name())
                .withOpenstackParameters(parameters);
    }

    @Override
    public String region() {
        return openstackProperties.getRegion();
    }

    @Override
    public String location() {
        return openstackProperties.getLocation();
    }

    @Override
    public String availabilityZone() {
        return openstackProperties.getAvailabilityZone();
    }

    @Override
    public String getDefaultInstanceType(Architecture architecture) {
        return openstackProperties.getInstance().getType();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        StackAuthenticationV4Request request = stackAuthenticationEntity.getRequest();
        stackAuthenticationEntity.withPublicKeyId(StringUtils.isBlank(request.getPublicKeyId())
                ? openstackProperties.getPublicKeyId()
                : request.getPublicKeyId());
        stackAuthenticationEntity.withPublicKey(StringUtils.isBlank(request.getPublicKey())
                ? commonCloudProperties().getSshPublicKey()
                : request.getPublicKey());
        stackAuthenticationEntity.withLoginUserName(request.getLoginUserName());
        return stackAuthenticationEntity;
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
    }

    @Override
    public String getStorageOptimizedInstanceType() {
        return openstackProperties.getInstance().getType();
    }

    @Override
    public VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto;
    }

    @Override
    public VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto;
    }

    @Override
    public VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto;
    }

    @Override
    public boolean verticalScalingSupported() {
        return false;
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
    public ExternalizedComputeClusterTestDto externalizedComputeCluster(ExternalizedComputeClusterTestDto computeClusterDto) {
        return computeClusterDto;
    }

    @Override
    public String getFreeIpaRebuildFullBackup() {
        return null;
    }

    @Override
    public String getFreeIpaRebuildDataBackup() {
        return null;
    }

    @Override
    public String getFreeIpaImageCatalog() {
        return openstackProperties.getFreeipaImage().getCatalog();
    }

    @Override
    public String getFreeIpaImageId() {
        return openstackProperties.getFreeipaImage().getId();
    }

    @Override
    public String getSdxImageCatalog() {
        return openstackProperties.getSdxImage().getCatalog();
    }

    @Override
    public String getSdxImageId() {
        return openstackProperties.getSdxImage().getId();
    }

    @Override
    public String getFreeIpaImageCatalogUrl() {
        return openstackProperties.getFreeipaImage().getUrl();
    }

    @Override
    public String getVariant() {
        return null;
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return null;
    }

    @Override
    public String getFreeIpaCentos7UpgradeImageId() {
        return null;
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageId() {
        return null;
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return null;
    }

    @Override
    public String getFreeIpaMarketplaceUpgradeImageCatalog() {
        return null;
    }

    @Override
    public String getSdxMarketplaceUpgradeImageId() {
        return null;
    }

    @Override
    public String getSdxMarketplaceUpgradeImageCatalog() {
        return null;
    }

    @Override
    public String getFreeIpaInstanceType() {
        return openstackProperties.getInstance().getType();
    }

    @Override
    public String getBaseLocationForPreTermination() {
        return getBaseLocation();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return commonClusterManagerProperties().getRuntimeVersion();
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
        return String.join("/", trimObjectName(DEFAULT_STORAGE_NAME), getSuiteName(), getTestName());
    }

    @Override
    public String getModifyDiskVolumeType() {
        return null;
    }

    @Override
    public String getAddDiskVolumeType() {
        return null;
    }

    @Override
    public String getDatahubInstanceType(String name) {
        return null;
    }

    @Override
    public String getDatalakeInstanceType(String name) {
        return null;
    }

    @Override
    public String getLatestMarketplacePreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto,
            CloudbreakClient cloudbreakClient, String runtimeVersion) {
        return null;
    }

    @Override
    public ResourceType getRootDiskResourceType() {
        return ResourceType.OPENSTACK_ATTACHED_DISK;
    }

    @Override
    public EncryptionProfileTestDto encryptionProfile(EncryptionProfileTestDto encryptionProfile) {
        return encryptionProfile;
    }
}
