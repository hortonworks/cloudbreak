package com.sequenceiq.it.cloudbreak.cloud.v4.gcp;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;

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
    public NetworkV4TestDto network(NetworkV4TestDto network) {
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
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getBlueprintName() {
        return gcpProperties.getDefaultBlueprintName();
    }
}
