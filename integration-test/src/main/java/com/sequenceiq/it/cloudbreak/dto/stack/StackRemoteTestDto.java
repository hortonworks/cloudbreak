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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
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
        if (sdx != null && sdx.getResponse() != null) {
            resourceCrn = sdx.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException("SDX is not available for remote command execution request!" +
                    " Please create a valid sdx resource first via TestContext in test steps!");
        }
        return this;
    }

    public StackRemoteTestDto withDistrox(DistroXTestDto distrox) {
        if (distrox != null && distrox.getResponse() != null) {
            resourceCrn = distrox.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException("DistroX is not available for remote command execution request!" +
                    " Please create a valid distroX resource first via TestContext in test steps!");
        }
        return this;
    }

    public StackRemoteTestDto withSdx(String key) {
        return withKey(key);
    }

    public StackRemoteTestDto withDistrox(String key) {
        return withKey(key);
    }

    private StackRemoteTestDto withKey(String key) {
        if (key != null && getTestContext().get(key) != null) {
            resourceCrn = getTestContext().get(key).getCrn();
        } else {
            throw new IllegalArgumentException(String.format("%s is not available for remote command execution request!" +
                    " Please create a valid resource first via TestContext in test steps!", key));
        }
        return this;
    }

    public StackRemoteTestDto withSdx() {
        SdxTestDto sdxTestDto = getTestContext().get(SdxTestDto.class);
        SdxInternalTestDto sdxInternalTestDto = getTestContext().get(SdxInternalTestDto.class);
        if (sdxTestDto != null && sdxTestDto.getResponse() != null) {
            resourceCrn = sdxTestDto.getResponse().getCrn();
        } else if (sdxInternalTestDto != null && sdxInternalTestDto.getResponse() != null) {
            resourceCrn = sdxInternalTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException("SDX is not available for remote command execution request!" +
                    " Please create a valid sdx resource first via TestContext in test steps!");
        }
        return this;
    }

    public StackRemoteTestDto withDistrox() {
        DistroXTestDto distroXTestDto = getTestContext().get(DistroXTestDto.class);
        if (distroXTestDto != null && distroXTestDto.getResponse() != null) {
            resourceCrn = distroXTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException("DistroX is not available for remote command execution request!" +
                    " Please create a valid distroX resource first via TestContext in test steps!");
        }
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
        DistroXTestDto distroXTestDto = getTestContext().get(DistroXTestDto.class);
        if (distroXTestDto != null && distroXTestDto.getResponse() != null) {
            return getHostgroupHosts(hostGroupName, distroXTestDto.getResponse());
        } else {
            throw new IllegalArgumentException("DistroX is not available for getting its hosts!" +
                    " Please create a valid distroX resource first via TestContext in test steps!");
        }
    }

    public Set<String> getSdxHosts(String hostGroupName) {
        SdxTestDto sdxTestDto = getTestContext().get(SdxTestDto.class);
        SdxInternalTestDto sdxInternalTestDto = getTestContext().get(SdxInternalTestDto.class);
        if (sdxTestDto != null && sdxTestDto.getResponse() != null) {
            return getHostgroupHosts(hostGroupName, sdxTestDto.getResponse().getStackV4Response());
        } else if (sdxInternalTestDto != null && sdxInternalTestDto.getResponse() != null) {
            return getHostgroupHosts(hostGroupName, sdxInternalTestDto.getResponse().getStackV4Response());
        } else {
            throw new IllegalArgumentException("SDX is not available for getting its hosts!" +
                    " Please create a valid sdx resource first via TestContext in test steps!");
        }
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
