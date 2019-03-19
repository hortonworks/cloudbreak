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
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

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
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(getTestParameter().getWithDefault(GcpParameters.Instance.TYPE, "n1-standard-8"));
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(GcpParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(GcpParameters.Instance.VOLUME_COUNT, "1"));
        String attachedVolumeType = getTestParameter().getWithDefault(GcpParameters.Instance.VOLUME_TYPE, "pd-standard");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setNoFirewallRules(false);
        gcpNetworkV4Parameters.setNoPublicIp(false);
        return network.withGcp(gcpNetworkV4Parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withGcp(stackParameters());
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return cluster
                .withValidateClusterDefinition(Boolean.TRUE)
                .withClusterDefinitionName(getClusterDefinitionName());
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
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getClusterDefinitionName() {
        return getTestParameter().getWithDefault(CommonCloudParameters.CLUSTER_DEFINITION_NAME, GcpParameters.DEFAULT_CLUSTER_DEFINTION_NAME);
    }
}
