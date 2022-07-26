package com.sequenceiq.it.cloudbreak.dto.stack;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionRequest;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Prototype
public class StackRemoteTestDto extends AbstractCloudbreakTestDto<RemoteCommandsExecutionRequest, RemoteCommandsExecutionResponse, StackRemoteTestDto> {

    private static final String STACK_REMOTE = "STACK_REMOTE";

    private String resourceCrn;

    public StackRemoteTestDto(TestContext testContext) {
        super(new RemoteCommandsExecutionRequest(), testContext);
    }

    public StackRemoteTestDto() {
        super(STACK_REMOTE);
    }

    public StackRemoteTestDto valid() {
        return withCommand("ls -l").withHostGroups();
    }

    public StackRemoteTestDto withHostGroups() {
        getRequest().setHostGroups(Set.of(MASTER.getName()));
        return this;
    }

    public StackRemoteTestDto withHostGroups(Set<String> hostGroups) {
        getRequest().setHostGroups(hostGroups);
        return this;
    }

    public StackRemoteTestDto withCommand(String command) {
        getRequest().setCommand(command);
        return this;
    }

    public StackRemoteTestDto withHosts(Set<String> hosts) {
        getRequest().setHosts(hosts);
        return this;
    }

    public StackRemoteTestDto withSdx(SdxTestDto sdx) {
        resourceCrn = sdx.getResponse().getCrn();
        return this;
    }

    public StackRemoteTestDto withDistrox(DistroXTestDto distrox) {
        resourceCrn = distrox.getResponse().getCrn();
        return this;
    }

    public StackRemoteTestDto withSdx(String key) {
        resourceCrn = getTestContext().get(key).getCrn();
        return this;
    }

    public StackRemoteTestDto withDistrox(String key) {
        resourceCrn = getTestContext().get(key).getCrn();
        return this;
    }

    public StackRemoteTestDto withSdx() {
        resourceCrn = getTestContext().get(SdxTestDto.class).getCrn();
        return this;
    }

    public StackRemoteTestDto withDistrox() {
        resourceCrn = getTestContext().get(DistroXTestDto.class).getCrn();
        return this;
    }

    public String getResourceCrn() {
        if (Crn.isCrn(resourceCrn)) {
            return resourceCrn;
        } else {
            throw new IllegalArgumentException("Resource CRN has not been defined for remote command execution request!" +
                    " Please define this via 'withSdx' or 'withDistrox' methods in test steps!");
        }
    }

    public Set<String> getDistroxHosts() {
        return getDistroxHosts(MASTER.getName());
    }

    public Set<String> getSdxHosts() {
        return getSdxHosts(MASTER.getName());
    }

    public Set<String> getDistroxHosts(String hostGroupName) {
        return getHostgroupHosts(hostGroupName, getTestContext().get(DistroXTestDto.class).getResponse());
    }

    public Set<String> getSdxHosts(String hostGroupName) {
        return getHostgroupHosts(hostGroupName, getTestContext().get(SdxTestDto.class).getResponse().getStackV4Response());
    }

    private Set<String> getHostgroupHosts(String hostGroupName, StackV4Response stackV4Response) {
        InstanceGroupV4Response instanceGroupV4Response = stackV4Response.getInstanceGroups().stream()
                .filter(instanceGroup ->
                        instanceGroup.getName().equals(hostGroupName)).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupV4Response)
                .getMetadata().stream().map(InstanceMetaDataV4Response::getDiscoveryFQDN).collect(Collectors.toSet());
    }

    @Override
    public StackRemoteTestDto when(Action<StackRemoteTestDto, CloudbreakClient> action) {
        return getTestContext().when(this, CloudbreakClient.class, action, emptyRunningParameter());
    }

    @Override
    public StackRemoteTestDto then(Assertion<StackRemoteTestDto, CloudbreakClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public StackRemoteTestDto then(Assertion<StackRemoteTestDto, CloudbreakClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then(this, CloudbreakClient.class, assertion, runningParameter);
    }
}
