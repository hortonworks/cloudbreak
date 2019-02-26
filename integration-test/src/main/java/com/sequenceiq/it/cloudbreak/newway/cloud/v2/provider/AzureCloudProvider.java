package com.sequenceiq.it.cloudbreak.newway.cloud.v2.provider;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters.CREDENTIAL_DEFAULT_DESCRIPTION;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBased;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.AwsParameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.AzureParameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

@Component
public class AzureCloudProvider extends AbstractCloudProvider {

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        AzureCredentialV4Parameters parameters = new AzureCredentialV4Parameters();
        parameters.setSubscriptionId(getTestParameter().getRequired(AzureParameters.Credential.SUBSCRIPTION_ID));
        parameters.setTenantId(getTestParameter().getRequired(AzureParameters.Credential.TENANT_ID));
        AppBased appBased = new AppBased();
        appBased.setAccessKey(getTestParameter().getRequired(AzureParameters.Credential.APP_ID));
        appBased.setSecretKey(getTestParameter().getRequired(AzureParameters.Credential.APP_PASSWORD));
        parameters.setAppBased(appBased);
        return credential.withAzureParameters(parameters)
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION);
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault(AzureParameters.REGION, "West Europe");
    }

    @Override
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return template.withInstanceType(getTestParameter().getWithDefault(AzureParameters.Instance.TYPE, "Standard_D12_v2"));
    }

    @Override
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_COUNT, "1"));
        String attachedVolumeType = getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_TYPE, "Standard_LRS");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
        AzureNetworkV4Parameters parameters = new AzureNetworkV4Parameters();
        parameters.setNoPublicIp(false);
        parameters.setNoFirewallRules(false);
        network.withAzure(parameters);
        network.withSubnetCIDR(getSubnetCIDR());
        return network;
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
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        stackAuthenticationEntity.withPublicKey(sshPublicKey);
        return stackAuthenticationEntity;
    }

    @Override
    public String getDefaultClusterDefinitionName() {
        return AzureParameters.DEFAULT_CLUSTER_DEFINTION_NAME;
    }
}
