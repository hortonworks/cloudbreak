package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import static java.lang.String.format;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXVolumeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentSecurityAccessTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

@Component
public class GcpCloudProvider extends AbstractCloudProvider {

    private static final String JSON_CREDENTIAL_TYPE = "json";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudProvider.class);

    @Inject
    private GcpProperties gcpProperties;

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
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setNoFirewallRules(false);
        gcpNetworkV4Parameters.setNoPublicIp(false);
        gcpNetworkV4Parameters.setSharedProjectId(gcpProperties.getSharedProjectId());
        return network.withGcp(gcpNetworkV4Parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withSubnetIDs(gcpProperties.getSubnetIds())
                .withGcp(environmentNetworkParameters());
    }

    private EnvironmentNetworkGcpParams environmentNetworkParameters() {
        EnvironmentNetworkGcpParams params = new EnvironmentNetworkGcpParams();
        params.setSharedProjectId(gcpProperties.getSharedProjectId());
        params.setNetworkId(gcpProperties.getNetworkId());
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
        return notImplementedException();
    }

    @Override
    public void setImageId(String id) {
        notImplementedException();
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
        GcpCredentialParameters parameters = new GcpCredentialParameters();
        String credentialType = gcpProperties.getCredential().getType();
        if (JSON_CREDENTIAL_TYPE.equalsIgnoreCase(credentialType)) {
            JsonParameters jsonParameters = new JsonParameters();
            jsonParameters.setCredentialJson(gcpProperties.getCredential().getJson());
            parameters.setJson(jsonParameters);
        } else {
            P12Parameters p12Parameters = new P12Parameters();
            p12Parameters.setProjectId(gcpProperties.getCredential().getProjectId());
            p12Parameters.setServiceAccountId(gcpProperties.getCredential().getServiceAccountId());
            p12Parameters.setServiceAccountPrivateKey(gcpProperties.getCredential().getP12());
            parameters.setP12(p12Parameters);
        }
        return credential.withGcpParameters(parameters)
                .withCloudPlatform(CloudPlatform.GCP.name())
                .withDescription(commonCloudProperties().getDefaultCredentialDescription());
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
        return cloudStorage;
    }

    @Override
    public FileSystemType getFileSystemType() {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        return gcsCloudStorageV1Parameters.getType();
    }

    @Override
    public String getBaseLocation() {
        return null;
    }

    public String getInstanceProfile() {
        return null;
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return imageSettings.withImageCatalog(commonCloudProperties().getImageCatalogName());
    }

    @Override
    public String getPreviousPreWarmedImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient) {
        return notImplementedException();
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

    private <T> T notImplementedException() {
        throw new NotImplementedException(String.format("Not implemented on %s", getCloudPlatform()));
    }

}
