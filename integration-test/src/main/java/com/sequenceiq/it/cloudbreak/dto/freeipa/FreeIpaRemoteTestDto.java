package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionRequest;
import com.sequenceiq.common.api.command.RemoteCommandsExecutionResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaRemoteTestDto extends AbstractFreeIpaTestDto<RemoteCommandsExecutionRequest, RemoteCommandsExecutionResponse, FreeIpaRemoteTestDto>
        implements EnvironmentAware {

    private static final String FREEIPA_REMOTE = "FREEIPA_REMOTE";

    public FreeIpaRemoteTestDto(TestContext testContext) {
        super(new RemoteCommandsExecutionRequest(), testContext);
    }

    public FreeIpaRemoteTestDto() {
        super(FREEIPA_REMOTE);
    }

    public FreeIpaRemoteTestDto valid() {
        return withCommand("ls -l").withHostGroups();
    }

    public FreeIpaRemoteTestDto withHostGroups() {
        getRequest().setHostGroups(Set.of(MASTER.getName()));
        return this;
    }

    public FreeIpaRemoteTestDto withCommand(String command) {
        getRequest().setCommand(command);
        return this;
    }

    public FreeIpaRemoteTestDto withHosts(Set<String> hosts) {
        getRequest().setHosts(hosts);
        return this;
    }

    public FreeIpaRemoteTestDto withHosts() {
        getRequest().setHosts(getHosts());
        return this;
    }

    private Set<String> getHosts() {
        InstanceGroupResponse instanceGroupResponse =
                getClientForCleanup().getDefaultClient().getFreeIpaV1Endpoint().describe(getEnvironmentCrn())
                        .getInstanceGroups().stream()
                        .filter(instanceGroup ->
                                instanceGroup.getName().equals(MASTER.getName())).findFirst().orElse(null);
        return Objects.requireNonNull(instanceGroupResponse)
                .getMetaData().stream().map(InstanceMetaDataResponse::getDiscoveryFQDN).collect(Collectors.toSet());
    }

    @Override
    public String getEnvironmentCrn() {
        String environmentCrn = getTestContext().get(EnvironmentTestDto.class).getCrn();
        if (Crn.isCrn(environmentCrn)) {
            return environmentCrn;
        } else {
            throw new IllegalArgumentException("Environment CRN has not been defined for remote command execution request!" +
                    " Please create a valid environment in test steps!");
        }
    }

    @Override
    public FreeIpaRemoteTestDto when(Action<FreeIpaRemoteTestDto, FreeIpaClient> action) {
        return getTestContext().when(this, FreeIpaClient.class, action, emptyRunningParameter());
    }

    @Override
    public FreeIpaRemoteTestDto then(Assertion<FreeIpaRemoteTestDto, FreeIpaClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public FreeIpaRemoteTestDto then(Assertion<FreeIpaRemoteTestDto, FreeIpaClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then(this, FreeIpaClient.class, assertion, runningParameter);
    }
}
