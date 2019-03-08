package com.sequenceiq.it.cloudbreak.newway.cloud.v2.gcp;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters.CREDENTIAL_DEFAULT_DESCRIPTION;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.JsonParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudParameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws.AwsParameters;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

@Component
public class GcpCloudProvider extends AbstractCloudProvider {

    @Override
    public String region() {
        return getTestParameter().getWithDefault(GcpParameters.REGION, "europe-west2");
    }

    @Override
    public String location() {
        return getTestParameter().getWithDefault(GcpParameters.REGION, "europe-west2");
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(GcpParameters.AVAILABILITY_ZONE, "europe-west2-a");
    }

    @Override
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return template.withInstanceType(getTestParameter().getWithDefault(GcpParameters.Instance.TYPE, "n1-standard-8"));
    }

    @Override
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_COUNT, "1"));
        String attachedVolumeType = getTestParameter().getWithDefault(AwsParameters.Instance.VOLUME_TYPE, "pd-standard");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setNoFirewallRules(false);
        gcpNetworkV4Parameters.setNoPublicIp(false);
        return network.withGcp(gcpNetworkV4Parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public StackV4EntityBase stack(StackV4EntityBase stack) {
        return stack.withGcp(stackParameters());
    }

    @Override
    public GcpStackV4Parameters stackParameters() {
        return new GcpStackV4Parameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        String defaultType = "json";
        String credentialType = getTestParameter().getWithDefault(GcpParameters.Credential.TYPE, defaultType);
        if (defaultType.equalsIgnoreCase(credentialType)) {
            JsonParameters jsonParameters = new JsonParameters();
            jsonParameters.setCredentialJson(getTestParameter().getRequired(GcpParameters.Credential.JSON));
            parameters.setJson(jsonParameters);
        } else {
            P12Parameters p12Parameters = new P12Parameters();
            p12Parameters.setProjectId(getTestParameter().getRequired(GcpParameters.Credential.PROJECT_ID));
            p12Parameters.setServiceAccountId(getTestParameter().getRequired(GcpParameters.Credential.SERVICE_ACCOUNT_ID));
            p12Parameters.setServiceAccountPrivateKey(getTestParameter().getRequired(GcpParameters.Credential.P12));
            parameters.setP12(p12Parameters);
        }
        return credential.withGcpParameters(parameters)
                .withCloudPlatform(CloudPlatform.GCP.name())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION);
    }

    @Override
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getDefaultClusterDefinitionName() {
        return GcpParameters.DEFAULT_CLUSTER_DEFINTION_NAME;
    }
}
