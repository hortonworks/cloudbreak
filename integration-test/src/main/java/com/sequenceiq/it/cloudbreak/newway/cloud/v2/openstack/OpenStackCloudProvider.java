package com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.AbstractCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDtoBase;

@Component
public class OpenStackCloudProvider extends AbstractCloudProvider {

    @Inject
    private OpenStackProperties openStackProperties;

    @Override
    public String region() {
        return openStackProperties.getRegion();
    }

    @Override
    public String location() {
        return openStackProperties.getLocation();
    }

    @Override
    public String availabilityZone() {
        return openStackProperties.getAvailabilityZone();
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(openStackProperties.getInstance().getType());
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = openStackProperties.getInstance().getVolumeSize();
        int attachedVolumeCount = openStackProperties.getInstance().getVolumeCount();
        String attachedVolumeType = openStackProperties.getInstance().getVolumeType();
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        OpenStackNetworkV4Parameters openStackNetworkV4Parameters = new OpenStackNetworkV4Parameters();
        openStackNetworkV4Parameters.setPublicNetId(openStackProperties.getPublicNetId());
        openStackNetworkV4Parameters.setNetworkingOption(openStackProperties.getNetworkingOption());
        return network.withOpenStack(openStackNetworkV4Parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withOpenStack(stackParameters());
    }

    @Override
    protected ClusterTestDto withCluster(ClusterTestDto cluster) {
        return cluster
                .withValidateBlueprint(Boolean.TRUE)
                .withBlueprintName(getBlueprintName());
    }

    @Override
    public OpenStackStackV4Parameters stackParameters() {
        return new OpenStackStackV4Parameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        OpenstackCredentialV4Parameters parameters = new OpenstackCredentialV4Parameters();
        parameters.setEndpoint(openStackProperties.getCredential().getEndpoint());
        parameters.setUserName(openStackProperties.getCredential().getUserName());
        parameters.setPassword(openStackProperties.getCredential().getPassword());
        KeystoneV2Parameters keystoneV2Parameters = new KeystoneV2Parameters();
        keystoneV2Parameters.setTenantName(openStackProperties.getCredential().getTenant());
        parameters.setKeystoneV2(keystoneV2Parameters);
        return credential.withCloudPlatform(getCloudPlatform().name())
                .withOpenstackParameters(parameters);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = commonCloudProperties().getSshPublicKey();
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getBlueprintName() {
        return openStackProperties.getDefaultBlueprintName();
    }
}
