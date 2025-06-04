package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.JsonParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.P12Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.RootVolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.SubnetId;
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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.gcp.GcpCloudFunctionality;

@Component
public class GcpCloudProvider extends AbstractCloudProvider {

    private static final String JSON_CREDENTIAL_TYPE = "json";

    private static final String DEFAULT_STORAGE_NAME = "apitest" + UUID.randomUUID().toString().replaceAll("-", "");

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudProvider.class);

    @Inject
    private GcpProperties gcpProperties;

    @Inject
    private GcpCloudFunctionality gcpCloudFunctionality;

    @Override
    public VerticalScalingTestDto freeIpaVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(gcpProperties.getVerticalScale().getFreeipa().getGroup())
                .withInstanceType(gcpProperties.getVerticalScale().getFreeipa().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto distroXVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(gcpProperties.getVerticalScale().getDatahub().getGroup())
                .withInstanceType(gcpProperties.getVerticalScale().getDatahub().getInstanceType());
    }

    @Override
    public VerticalScalingTestDto datalakeVerticalScalingTestDto(VerticalScalingTestDto verticalScalingTestDto) {
        return verticalScalingTestDto.withGroup(gcpProperties.getVerticalScale().getDatalake().getGroup())
                .withInstanceType(gcpProperties.getVerticalScale().getDatalake().getInstanceType());
    }

    @Override
    public boolean verticalScalingSupported() {
        return gcpProperties.getVerticalScale().isSupported();
    }

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
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template, Architecture architecture) {
        if (architecture != Architecture.X86_64) {
            throw new NotImplementedException(String.format("Architecture %s is not implemented", architecture.getName()));
        }
        return template.withInstanceType(gcpProperties.getInstance().getType());
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        EnvironmentTestDto result = super.environment(environment)
                .withFreeIpa(getAttachedFreeIpaRequest());
        if (StringUtils.isNotBlank(gcpProperties.getSecurityAccess().getDefaultSecurityGroup())) {
            securityAccessRequest.setDefaultSecurityGroupId(gcpProperties.getSecurityAccess().getDefaultSecurityGroup());
            result.withSecurityAccess(securityAccessRequest);
        }
        if (StringUtils.isNotBlank(gcpProperties.getSecurityAccess().getKnoxSecurityGroup())) {
            securityAccessRequest.setSecurityGroupIdForKnox(gcpProperties.getSecurityAccess().getKnoxSecurityGroup());
            result.withSecurityAccess(securityAccessRequest);
        }
        return result;
    }

    private AttachedFreeIpaRequest getAttachedFreeIpaRequest() {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        GcpFreeIpaParameters gcpFreeIpaParameters = new GcpFreeIpaParameters();
        attachedFreeIpaRequest.setGcp(gcpFreeIpaParameters);
        attachedFreeIpaRequest.setEnableMultiAz(isMultiAZ());
        return attachedFreeIpaRequest;
    }

    @Override
    public SdxInternalTestDto sdxInternal(SdxInternalTestDto sdxInternal) {
        sdxInternal.getRequest().getStackV4Request().setNetwork(null);
        return sdxInternal;
    }

    @Override
    public EnvironmentSecurityAccessTestDto environmentSecurityAccess(EnvironmentSecurityAccessTestDto environmentSecurityAccess) {
        EnvironmentSecurityAccessTestDto envSecAcc = super.environmentSecurityAccess(environmentSecurityAccess);
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
    public LoggingRequest loggingRequest(TelemetryTestDto dto) {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail(getServiceAccountEmail());
        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setGcs(gcsCloudStorageV1Parameters);
        loggingRequest.setStorageLocation(getBaseLocation());
        loggingRequest.setEnabledSensitiveStorageLogs(Set.of("SALT"));
        return loggingRequest;
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

    public String getNetworkId() {
        return gcpProperties.getNetwork().getNetworkId();
    }

    public String getSubnetId() {
        return gcpProperties.getNetwork().getSubnetId();
    }

    public Boolean getNoPublicIp() {
        return gcpProperties.getNetwork().getNoPublicIp();
    }

    public Boolean getNoFirewallRules() {
        return gcpProperties.getNetwork().getNoFirewallRules();
    }

    public String getSharedProjectId() {
        return gcpProperties.getNetwork().getSharedProjectId();
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setNoFirewallRules(gcpProperties.getNetwork().getNoFirewallRules());
        gcpNetworkV4Parameters.setNoPublicIp(gcpProperties.getNetwork().getNoPublicIp());
        String subnetCIDR = null;
        if (StringUtils.isNotBlank(gcpProperties.getNetwork().getSharedProjectId())) {
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
    public InstanceGroupNetworkV4Request instanceGroupNetworkV4Request(SubnetId subnetId) {
        if (isMultiAZ()) {
            InstanceGroupNetworkV4Request result = new InstanceGroupNetworkV4Request();
            result.createGcp();
            result.getGcp().setSubnetIds(subnetId.collectSubnets(gcpProperties.getNetwork().getSubnetIds()));
            return result;
        } else {
            return null;
        }
    }

    @Override
    public InstanceGroupNetworkV1Request instanceGroupNetworkV1Request(SubnetId subnetId) {
        if (isMultiAZ()) {
            InstanceGroupNetworkV1Request result = new InstanceGroupNetworkV1Request();
            result.createAws();
            result.getGcp().setSubnetIds(subnetId.collectSubnets(gcpProperties.getNetwork().getSubnetIds()));
            return result;
        } else {
            return null;
        }
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        return network;
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        Set<String> subnets = gcpProperties.getNetwork().getSubnetIds() == null ? Set.of(gcpProperties.getNetwork().getSubnetId()) :
                new HashSet<>(gcpProperties.getNetwork().getSubnetIds());
        return network
                .withSubnetIDs(subnets)
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
                .withBlueprintName(getDataEngDistroXBlueprintName());
    }

    @Override
    protected DistroXClusterTestDto withCluster(DistroXClusterTestDto cluster) {
        return cluster.withBlueprintName(getDataEngDistroXBlueprintName());
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
    public String getStorageOptimizedInstanceType() {
        return gcpProperties.getStorageOptimizedInstance().getType();
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
        StackAuthenticationV4Request request = stackAuthenticationEntity.getRequest();
        stackAuthenticationEntity.withPublicKeyId(request.getPublicKeyId());
        stackAuthenticationEntity.withPublicKey(StringUtils.isBlank(request.getPublicKey())
                ? commonCloudProperties().getSshPublicKey()
                : request.getPublicKey());
        stackAuthenticationEntity.withLoginUserName(request.getLoginUserName());
        return stackAuthenticationEntity;
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
        return String.join("/", gcpProperties.getCloudStorage().getBaseLocation(), trimObjectName(DEFAULT_STORAGE_NAME),
                getSuiteName(), getTestName());
    }

    @Override
    public String getBaseLocationForPreTermination() {
        return String.join("/", gcpProperties.getCloudStorage().getBaseLocation(),
                "pre-termination");
    }

    public String getServiceAccountEmail() {
        return gcpProperties.getCloudStorage().getGcs().getServiceAccountEmail();
    }

    public String getImageCatalogUrl() {
        return commonCloudProperties().getImageCatalogUrl();
    }

    private <T> T notImplementedException() {
        throw new NotImplementedException(format("Not implemented on %s", getCloudPlatform()));
    }

    @Override
    public String getFreeIpaUpgradeImageId() {
        return gcpProperties.getFreeipa().getUpgrade().getImageId();
    }

    @Override
    public String getFreeIpaUpgradeImageCatalog() {
        return gcpProperties.getFreeipa().getUpgrade().getCatalog();
    }

    @Override
    public EnvironmentTestDto withResourceEncryption(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto.withGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey(getEncryptionKey(true))
                        .build())
                .build());
    }

    @Override
    public EnvironmentTestDto withDatabaseEncryptionKey(EnvironmentTestDto environmentTestDto) {
        return withResourceEncryption(environmentTestDto);
    }

    @Override
    public EnvironmentTestDto withResourceEncryptionUserManagedIdentity(EnvironmentTestDto environmentTestDto) {
        return environmentTestDto;
    }

    @Override
    public DistroXTestDtoBase withResourceEncryption(DistroXTestDtoBase distroXTestDtoBase) {
        return distroXTestDtoBase;
    }

    public String getEncryptionKey(boolean environmentEncryption) {
        return environmentEncryption
                ? gcpProperties.getDiskEncryption().getEnvironmentKey()
                : gcpProperties.getDiskEncryption().getDatahubKey();
    }

    @Override
    public void verifyDiskEncryptionKey(DetailedEnvironmentResponse environment, String environmentName) {
        String encryptionKey = environment.getGcp().getGcpResourceEncryptionParameters().getEncryptionKey();
        if (StringUtils.isBlank(encryptionKey)) {
            LOGGER.error(format("KMS key is not available for '%s' environment!", environmentName));
            throw new TestFailException(format("KMS key is not available for '%s' environment!", environmentName));
        } else {
            LOGGER.info(format("Environment '%s' create has been done with '%s' KMS key.", environmentName, encryptionKey));
            Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' KMS key. ", environmentName, encryptionKey));
        }
    }

    @Override
    public void verifyVolumeEncryptionKey(List<String> volumeKmsKeyIds, String environmentName) {
        String kmsKey = getEncryptionKey(true);
        if (volumeKmsKeyIds.stream().noneMatch(keyId -> StringUtils.containsIgnoreCase(keyId, kmsKey))) {
            LOGGER.error(format("Volume has NOT been encrypted with '%s' KMS key!", kmsKey));
            throw new TestFailException(format("Volume has NOT been encrypted with '%s' KMS key!", kmsKey));
        } else {
            LOGGER.info(format("Volume has been encrypted with '%s' KMS key.", kmsKey));
            Log.then(LOGGER, format(" Volume has been encrypted with '%s' KMS key. ", kmsKey));
        }
    }

    @Override
    public boolean isMultiAZ() {
        return gcpProperties.getMultiaz();
    }

    @Override
    public boolean isExternalDatabaseSslEnforcementSupported() {
        return gcpProperties.getExternalDatabaseSslEnforcementSupported();
    }

    @Override
    public String getEmbeddedDbUpgradeSourceVersion() {
        return gcpProperties.getEmbeddedDbUpgradeSourceVersion();
    }

    @Override
    public String getFreeIpaRebuildFullBackup() {
        return gcpProperties.getFreeipa().getRebuild().getFullbackup();
    }

    @Override
    public String getFreeIpaRebuildDataBackup() {
        return gcpProperties.getFreeipa().getRebuild().getDatabackup();
    }
}
