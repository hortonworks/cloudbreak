package com.sequenceiq.it.cloudbreak.cloud.v4.azure;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.old.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;

@Component
public class AzureCloudProvider extends AbstractCloudProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudProvider.class);

    private static final String DEFAULT_STORAGE_NAME = "testsdx" + UUID.randomUUID().toString().replaceAll("-", "");

    @Inject
    private AzureProperties azureProperties;

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        AzureCredentialRequestParameters parameters = new AzureCredentialRequestParameters();
        parameters.setSubscriptionId(azureProperties.getCredential().getSubscriptionId());
        parameters.setTenantId(azureProperties.getCredential().getTenantId());
        AppBasedRequest appBased = new AppBasedRequest();
        appBased.setAccessKey(azureProperties.getCredential().getAppId());
        appBased.setSecretKey(azureProperties.getCredential().getAppPassword());
        parameters.setAppBased(appBased);
        return credential.withAzureParameters(parameters)
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withDescription(commonCloudProperties().getDefaultCredentialDescription());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withAzure(stackParameters());
    }

    @Override
    public DistroXTestDtoBase distrox(DistroXTestDtoBase distrox) {
        return distrox.withAzure(distroXParameters());
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
    public AzureStackV4Parameters stackParameters() {
        return new AzureStackV4Parameters();
    }

    @Override
    public CloudFunctionality getCloudFunctionality() {
        return null;
    }

    public AzureDistroXV1Parameters distroXParameters() {
        return new AzureDistroXV1Parameters();
    }

    @Override
    public String region() {
        return azureProperties.getRegion();
    }

    @Override
    public String location() {
        return azureProperties.getLocation();
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(azureProperties.getInstance().getType());
    }

    @Override
    public DistroXInstanceTemplateTestDto template(DistroXInstanceTemplateTestDto template) {
        return template.withInstanceType(azureProperties.getInstance().getType());
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = azureProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = azureProperties.getInstance().getVolumeCount();
        String attachedVolumeType = azureProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public DistroXVolumeTestDto attachedVolume(DistroXVolumeTestDto volume) {
        int attachedVolumeSize = azureProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = azureProperties.getInstance().getVolumeCount();
        String attachedVolumeType = azureProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        AzureNetworkV4Parameters parameters = new AzureNetworkV4Parameters();
        parameters.setNoPublicIp(false);
        parameters.setNoFirewallRules(false);
        return network.withAzure(parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public DistroXNetworkTestDto network(DistroXNetworkTestDto network) {
        AzureNetworkV1Parameters parameters = new AzureNetworkV1Parameters();
        return network.withAzure(parameters);
    }

    @Override
    public EnvironmentNetworkTestDto network(EnvironmentNetworkTestDto network) {
        return network.withNetworkCIDR(getSubnetCIDR())
                .withSubnetIDs(getSubnetIDs())
                .withAzure(environmentNetworkParameters());    }

    private EnvironmentNetworkAzureParams environmentNetworkParameters() {
        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setNetworkId(getNetworkId());
        environmentNetworkAzureParams.setNoFirewallRules(getNoFirewallRules());
        environmentNetworkAzureParams.setNoPublicIp(getNoPublicIp());
        environmentNetworkAzureParams.setResourceGroupName(getResourceGroupName());
        return environmentNetworkAzureParams;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public String availabilityZone() {
        return azureProperties.getAvailabilityZone();
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    public String getNetworkId() {
        return azureProperties.getNetwork().getNetworkId();
    }

    public Set<String> getSubnetIDs() {
        return azureProperties.getNetwork().getSubnetIds();
    }

    public Boolean getNoFirewallRules() {
        return azureProperties.getNetwork().getNoFirewallRules();
    }

    public Boolean getNoPublicIp() {
        return azureProperties.getNetwork().getNoPublicIp();
    }

    public String getResourceGroupName() {
        return azureProperties.getNetwork().getResourceGroupName();
    }

    @Override
    public String getBlueprintName() {
        return azureProperties.getDefaultBlueprintName();
    }

    @Override
    public String getBlueprintCdhVersion() {
        return azureProperties.getBlueprintCdhVersion();
    }

    @Override
    public TelemetryTestDto telemetry(TelemetryTestDto telemetry) {
        return telemetry;
    }

    @Override
    public SdxCloudStorageTestDto cloudStorage(SdxCloudStorageTestDto cloudStorage) {
        return cloudStorage
                .withFileSystemType(getFileSystemType())
                .withBaseLocation(getBaseLocation())
                .withAdlsGen2(adlsGen2CloudStorageParameters());
    }

    public AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters() {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setSecure(getSecure());
        adlsGen2CloudStorageV1Parameters.setManagedIdentity(getAssumerIdentity());
        return adlsGen2CloudStorageV1Parameters;
    }

    @Override
    public FileSystemType getFileSystemType() {
        AdlsCloudStorageV1Parameters adlsCloudStorageV1Parameters = new AdlsCloudStorageV1Parameters();
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        WasbCloudStorageV1Parameters wasbCloudStorageV1Parameters = new WasbCloudStorageV1Parameters();
        FileSystemType fileSystemType;

        switch (azureProperties.getCloudstorage().getFileSystemType()) {
            case "WASB_INTEGRATED":
            case "WASB":
                fileSystemType = wasbCloudStorageV1Parameters.getType();
                break;
            case "ADLS":
                fileSystemType = adlsCloudStorageV1Parameters.getType();
                break;
            case "ADLS_GEN_2":
                fileSystemType = adlsGen2CloudStorageV1Parameters.getType();
                break;
            default:
                LOGGER.warn("The given {} File System Type is not in the list of Azure file system types. So we use the default one; {}",
                        azureProperties.getCloudstorage().getFileSystemType(), "ADLS_GEN_2");
                fileSystemType = adlsGen2CloudStorageV1Parameters.getType();
                break;
        }

        return fileSystemType;
    }

    @Override
    public String getBaseLocation() {
        return azureProperties.getCloudstorage().getBaseLocation();
    }

    public String getAssumerIdentity() {
        return azureProperties.getCloudstorage().getAdlsGen2().getAssumerIdentity();
    }

    public String getLoggerIdentity() {
        return azureProperties.getCloudstorage().getAdlsGen2().getLoggerIdentity();
    }

    public Boolean getSecure() {
        return azureProperties.getCloudstorage().getSecure();
    }

    public String getAccountName() {
        return azureProperties.getCloudstorage().getAccountName();
    }

    public String getAccountKey() {
        return azureProperties.getCloudstorage().getAccountKey();
    }
}
