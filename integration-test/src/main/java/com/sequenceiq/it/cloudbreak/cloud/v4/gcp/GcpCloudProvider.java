package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.JsonParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.P12Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.gcp.GcpCloudFunctionality;

@Component
public class GcpCloudProvider extends AbstractCloudProvider {

    private static final String JSON_CREDENTIAL_TYPE = "json";

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudProvider.class);

    @Inject
    private GcpProperties gcpProperties;

    @Inject
    private GcpCloudFunctionality gcpCloudFunctionality;

    @Override
    public String region() {
        return gcpProperties.getRegion();
    }

    @Override
    public String location() {
        return gcpProperties.getLocation();
    }

    @Override
    public String availabilityZone() {
        return gcpProperties.getAvailabilityZone();
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(gcpProperties.getInstance().getType());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return template.withInstanceType(gcpProperties.getInstance().getType());
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        EnvironmentTestDto result = super.environment(environment);
        if (!StringUtils.isEmpty(gcpProperties.getSecurityAccess().getDefaultSecurityGroup())) {
            securityAccessRequest.setDefaultSecurityGroupId(gcpProperties.getSecurityAccess().getDefaultSecurityGroup());
            result.withSecurityAccess(securityAccessRequest);
        }
        if (!StringUtils.isEmpty(gcpProperties.getSecurityAccess().getKnoxSecurityGroup())) {
            securityAccessRequest.setSecurityGroupIdForKnox(gcpProperties.getSecurityAccess().getKnoxSecurityGroup());
            result.withSecurityAccess(securityAccessRequest);
        }
        return result;
    }

    @Override
    public SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        sdxInternal.getRequest().getStackV4Request().setNetwork(null);
        return sdxInternal;
    }

    @Override
    public SdxCustomTestDto sdxCustom(SdxCustomTestDto sdxCustom) {
        return sdxCustom;
    }

    @Override
    public EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccessTestDto) {
        EnvironmentSecurityAccessTestDto envSecAcc = super.environmentSecurityAccess(environmentSecurityAccessTestDto);
        return envSecAcc.withDefaultSecurityGroupId(gcpProperties.getSecurityAccess().getDefaultSecurityGroup())
                .withSecurityGroupIdForKnox(gcpProperties.getSecurityAccess().getKnoxSecurityGroup());
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = gcpProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = gcpProperties.getInstance().getVolumeCount();
        String attachedVolumeType = gcpProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        int attachedVolumeSize = gcpProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = gcpProperties.getInstance().getVolumeCount();
        String attachedVolumeType = gcpProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public RootVolumeV4TestDto rootVolume(RootVolumeV4TestDto rootVolume) {
        int rootVolumeSize = gcpProperties.getInstance().getRootVolumeSize();
        return rootVolume.withSize(rootVolumeSize);
    }

    @Override
    public DistroXRootVolumeTestDto distroXRootVolume(DistroXRootVolumeTestDto distroXRootVolume) {
        int rootVolumeSize = gcpProperties.getInstance().getRootVolumeSize();
        return distroXRootVolume.withSize(rootVolumeSize);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setNoFirewallRules(gcpProperties.getNetwork().getNoFirewallRules());
        gcpNetworkV4Parameters.setNoPublicIp(gcpProperties.getNetwork().getNoPublicIp());
        String subnetCIDR = null;
        if (!StringUtils.isEmpty(gcpProperties.getNetwork().getSharedProjectId())) {
            gcpNetworkV4Parameters.setSharedProjectId(gcpProperties.getNetwork().getSharedProjectId());
            gcpNetworkV4Parameters.setNetworkId(gcpProperties.getNetwork().getNetworkId());
            gcpNetworkV4Parameters.setSubnetId(gcpProperties.getNetwork().getSubnetId());
        } else {
            subnetCIDR = getSubnetCIDR();
        }
        return network
                .withGcp(gcpNetworkV4Parameters)
                .withSubnetCIDR(subnetCIDR);
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network
                .withSubnetIDs(Set.of(gcpProperties.getNetwork().getSubnetId()))
                .withGcp(environmentNetworkParameters());
    }

    private EnvironmentNetworkGcpParams environmentNetworkParameters() {
        EnvironmentNetworkGcpParams params = new EnvironmentNetworkGcpParams();
        params.setSharedProjectId(gcpProperties.getNetwork().getSharedProjectId());
        params.setNetworkId(gcpProperties.getNetwork().getNetworkId());
        params.setNoFirewallRules(gcpProperties.getNetwork().getNoFirewallRules());
        params.setNoPublicIp(gcpProperties.getNetwork().getNoPublicIp());
        return params;
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withGcp(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox;
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getBlueprintName());
    }

    @Override
    public GcpStackV4Parameters stackParameters() {
        return new GcpStackV4Parameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return gcpCloudFunctionality;
    }

    @Override
    public void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        GcpCredentialParameters parameters;
        String credentialType = gcpProperties.getCredential().getType();
        if (JSON_CREDENTIAL_TYPE.equalsIgnoreCase(credentialType)) {
            parameters = gcpCredentialParametersJson();
        } else {
            parameters = gcpCredentialParametersP12();
        }
        return credential.withGcpParameters(parameters)
                .withCloudPlatform(CloudPlatform.GCP.name())
                .withDescription(commonCloudProperties().getDefaultCredentialDescription());
    }

    public GcpCredentialParameters gcpCredentialParametersJson() {
        GcpCredentialParameters parameters = new GcpCredentialParameters();
        JsonParameters jsonCredentialParameters = new JsonParameters();
        String json = gcpProperties.getCredential().getJson().getBase64();
        jsonCredentialParameters.setCredentialJson(json);
        parameters.setJson(jsonCredentialParameters);
        return parameters;
    }

    public GcpCredentialParameters gcpCredentialParametersP12() {
        GcpCredentialParameters parameters = new GcpCredentialParameters();
        P12Parameters p12CredentialParameters = new P12Parameters();
        String serviceAccountId = gcpProperties.getCredential().getP12().getServiceAccountId();
        String projectId = gcpProperties.getCredential().getP12().getProjectId();
        String serviceAccountPrivateKey = gcpProperties.getCredential().getP12().getServiceAccountPrivateKey();
        p12CredentialParameters.setServiceAccountId(serviceAccountId);
        p12CredentialParameters.setProjectId(projectId);
        p12CredentialParameters.setServiceAccountPrivateKey(serviceAccountPrivateKey);
        parameters.setP12(p12CredentialParameters);
        return parameters;
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getBlueprintName() {
        return commonClusterManagerProperties().getInternalDistroXBlueprintName();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return commonClusterManagerProperties().getRuntimeVersion();
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage
                .withFileSystemType(getFileSystemType())
                .withBaseLocation(getBaseLocation())
                .withGcs(gcsCloudStorageParameters());
    }

    public GcsCloudStorageV1Parameters gcsCloudStorageParameters() {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(gcpProperties.getCloudStorage().getGcs().getServiceAccountEmail());
        return gcsCloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        return gcsCloudStorageV1Parameters.getType();
    }

    @Override
    public String getBaseLocation() {
        return String.join("/", gcpProperties.getCloudStorage().getBaseLocation(), DEFAULT_STORAGE_NAME);
    }

    public String getServiceAccountEmail() {
        return gcpProperties.getCloudStorage().getGcs().getServiceAccountEmail();
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings
                .withImageId(gcpProperties.getBaseimage().getImageId())
                .withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (gcpProperties.getBaseimage().getImageId() == null || gcpProperties.getBaseimage().getImageId().isEmpty()) {
            try {
                List<ImageV4Response> images = cloudbreakClient
                        .getDefaultClient()
                        .imageCatalogV4Endpoint()
                        .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                                CloudPlatform.GCP.name(), null, null).getCdhImages();

                ImageV4Response olderImage = images.get(images.size() - 2);
                Log.log(LOGGER, format(" Image Catalog Name: %s ", imageCatalogTestDto.getRequest().getName()));
                Log.log(LOGGER, format(" Image Catalog URL: %s ", imageCatalogTestDto.getRequest().getUrl()));
                Log.log(LOGGER, format(" Selected Pre-warmed Image Date: %s | ID: %s | Description: %s | Stack Version: %s ", olderImage.getDate(),
                        olderImage.getUuid(), olderImage.getStackDetails().getVersion(), olderImage.getDescription()));
                gcpProperties.getBaseimage().setImageId(olderImage.getUuid());

                return olderImage.getUuid();
            } catch (Exception e) {
                LOGGER.error("Cannot fetch pre-warmed images of {} image catalog!", imageCatalogTestDto.getRequest().getName());
                throw new TestFailException(" Cannot fetch pre-warmed images of " + imageCatalogTestDto.getRequest().getName() + " image catalog!", e);
            }
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", gcpProperties.getBaseimage().getImageId()));
            return gcpProperties.getBaseimage().getImageId();
        }
    }

    @Override
    public String getLatestBaseImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        if (gcpProperties.getBaseimage().getImageId() == null || gcpProperties.getBaseimage().getImageId().isEmpty()) {
            String imageId = getLatestBaseImage(imageCatalogTestDto, cloudbreakClient, CloudPlatform.GCP.name());
            gcpProperties.getBaseimage().setImageId(imageId);
            return imageId;
        } else {
            Log.log(LOGGER, format(" Image Catalog Name: %s ", commonCloudProperties().getImageCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", commonCloudProperties().getImageCatalogUrl()));
            Log.log(LOGGER, format(" Image ID for SDX create: %s ", gcpProperties.getBaseimage().getImageId()));
            return gcpProperties.getBaseimage().getImageId();
        }
    }

    public String getImageCatalogUrl() {
        return commonCloudProperties().getImageCatalogUrl();
    }

    @Override
    public void setImageId(String id) {
        gcpProperties.getBaseimage().setImageId(id);
    }

    private <T> T notImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s", getCloudPlatform()));
    }

}
