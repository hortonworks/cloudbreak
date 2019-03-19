package com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackV4Parameters;
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
public class OpenStackCloudProvider extends AbstractCloudProvider {

    @Override
    public String region() {
        return getTestParameter().getWithDefault(OpenStackParameters.REGION, "RegionOne");
    }

    @Override
    public String location() {
        return getTestParameter().getWithDefault(OpenStackParameters.LOCATION, "Texas");
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault(OpenStackParameters.AVAILABILITY_ZONE, "nova");
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return template.withInstanceType(getTestParameter().getWithDefault(OpenStackParameters.Instance.TYPE, "m1.xlarge"));
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        int attachedVolumeSize = Integer.parseInt(getTestParameter().getWithDefault(OpenStackParameters.Instance.VOLUME_SIZE, "100"));
        int attachedVolumeCount = Integer.parseInt(getTestParameter().getWithDefault(OpenStackParameters.Instance.VOLUME_COUNT, "0"));
        String attachedVolumeType = getTestParameter().getWithDefault(OpenStackParameters.Instance.VOLUME_TYPE, "HDD");
        return volume.withSize(attachedVolumeSize)
                .withCount(attachedVolumeCount)
                .withType(attachedVolumeType);
    }

    @Override
    public NetworkV2TestDto network(NetworkV2TestDto network) {
        OpenStackNetworkV4Parameters openStackNetworkV4Parameters = new OpenStackNetworkV4Parameters();
        openStackNetworkV4Parameters.setPublicNetId(getTestParameter()
                .getWithDefault(OpenStackParameters.PUBLIC_NET_ID, "999e09bc-cf75-4a19-98fb-c0b4ddee6d93"));
        openStackNetworkV4Parameters.setNetworkingOption(getTestParameter()
                .getWithDefault(OpenStackParameters.NETWORKING_OPTION, "self-service"));
        return network.withOpenStack(openStackNetworkV4Parameters)
                .withSubnetCIDR(getSubnetCIDR());
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return stack.withOpenStack(stackParameters());
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return cluster
                .withValidateClusterDefinition(Boolean.TRUE)
                .withClusterDefinitionName(getClusterDefinitionName());
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
        parameters.setEndpoint(getTestParameter().getRequired(OpenStackParameters.Credential.ENDPOINT));
        parameters.setUserName(getTestParameter().getRequired(OpenStackParameters.Credential.USER_NAME));
        parameters.setPassword(getTestParameter().getRequired(OpenStackParameters.Credential.PASSWORD));
        KeystoneV2Parameters keystoneV2Parameters = new KeystoneV2Parameters();
        keystoneV2Parameters.setTenantName(getTestParameter().getRequired(OpenStackParameters.Credential.TENANT));
        parameters.setKeystoneV2(keystoneV2Parameters);
        return credential.withCloudPlatform(getCloudPlatform().name())
                .withOpenstackParameters(parameters);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        String sshPublicKey = getTestParameter().getWithDefault(CommonCloudParameters.SSH_PUBLIC_KEY, CommonCloudParameters.DEFAULT_SSH_PUBLIC_KEY);
        return stackAuthenticationEntity.withPublicKey(sshPublicKey);
    }

    @Override
    public String getClusterDefinitionName() {
        return getTestParameter().getWithDefault(CommonCloudParameters.CLUSTER_DEFINITION_NAME, OpenStackParameters.DEFAULT_CLUSTER_DEFINTION_NAME);
    }
}
