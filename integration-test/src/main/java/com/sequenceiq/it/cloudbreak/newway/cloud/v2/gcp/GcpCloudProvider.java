package com.sequenceiq.it.cloudbreak.newway.cloud.v2.gcp;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.JsonParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

@Component
public class GcpCloudProvider extends AbstractCloudProvider {

    private static final String JSON_CREDENTIAL_TYPE = "json";

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
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = gcpProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = gcpProperties.getInstance().getVolumeCount();
        String attachedVolumeType = gcpProperties.getInstance().getVolumeType();
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
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
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
        return gcpProperties.getDefaultBlueprintName();
    }
}
