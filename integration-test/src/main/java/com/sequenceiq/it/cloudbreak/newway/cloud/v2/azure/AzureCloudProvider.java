package com.sequenceiq.it.cloudbreak.newway.cloud.v2.azure;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters.CREDENTIAL_DEFAULT_DESCRIPTION;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBasedRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4RequestParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

@Component
public class AzureCloudProvider extends AbstractCloudProvider {

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        AzureCredentialV4RequestParameters parameters = new AzureCredentialV4RequestParameters();
        parameters.setSubscriptionId(getTestParameter().getRequired(AzureParameters.Credential.SUBSCRIPTION_ID));
        parameters.setTenantId(getTestParameter().getRequired(AzureParameters.Credential.TENANT_ID));
        AppBasedRequest appBased = new AppBasedRequest();
        appBased.setAccessKey(getTestParameter().getRequired(AzureParameters.Credential.APP_ID));
        appBased.setSecretKey(getTestParameter().getRequired(AzureParameters.Credential.APP_PASSWORD));
        parameters.setAppBased(appBased);
        return credential.withAzureParameters(parameters)
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION);
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withAzure(stackParameters());
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return cluster
                .withValidateClusterDefinition(Boolean.TRUE)
                .withClusterDefinitionName(getClusterDefinitionName());
    }

    @Override
    public AzureStackV4Parameters stackParameters() {
        return new AzureStackV4Parameters();
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault(AzureParameters.REGION, "West Europe");
    }

    @Override
    public String location() {
        return getTestParameter().getWithDefault(AzureParameters.LOCATION, "West Europe");
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(getTestParameter().getWithDefault(AzureParameters.Instance.TYPE, "Standard_D12_v2"));
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(AzureParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(AzureParameters.Instance.VOLUME_COUNT, "1"));
        String attachedVolumeType = getTestParameter().getWithDefault(AzureParameters.Instance.VOLUME_TYPE, "Standard_LRS");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        AzureNetworkV4Parameters parameters = new AzureNetworkV4Parameters();
        parameters.setNoPublicIp(false);
        parameters.setNoFirewallRules(false);
        return network.withAzure(parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(AzureParameters.AVAILABILITY_ZONE, null);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getClusterDefinitionName() {
        return getTestParameter().getWithDefault(CommonCloudParameters.CLUSTER_DEFINITION_NAME, AzureParameters.DEFAULT_CLUSTER_DEFINTION_NAME);
    }
}
