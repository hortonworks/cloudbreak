package com.sequenceiq.it.cloudbreak.assertion.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

@ExtendWith(MockitoExtension.class)
class StackAssertionTest {

    private static final List<String> GATEWAY_IPS = List.of("10.0.0.1", "10.0.0.2");

    private static final String CONFIG_FILE = "/etc/salt/master.d/minimum_auth_version.conf";

    @Mock
    private SshJClientActions sshJClientActions;

    @Mock
    private DistroXTestDto testDto;

    @InjectMocks
    private StackAssertion underTest;

    private StackV4Response response;

    @BeforeEach
    void setUp() {
        response = new StackV4Response();
        response.setInstanceGroups(List.of(
                group(InstanceGroupType.GATEWAY, instance("10.0.0.1", InstanceMetadataType.GATEWAY_PRIMARY),
                        instance("10.0.0.2", InstanceMetadataType.GATEWAY)),
                group(InstanceGroupType.CORE, instance("10.0.0.3", InstanceMetadataType.CORE))));
        when(testDto.getResponse()).thenReturn(response);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checksBothGatewaysButNotWorkers(boolean managed) {
        when(sshJClientActions.executeSshCommandOnHosts(eq(GATEWAY_IPS), anyString())).thenReturn(outcomes("Success"));

        validate(managed);

        String expectedCommand = managed
                ? "sudo grep -Pq '^minimum_auth_version: 2$' " + CONFIG_FILE + " && echo Success || echo Failure"
                : "sudo test ! -e " + CONFIG_FILE + " && echo Success || echo Failure";
        verify(sshJClientActions).executeSshCommandOnHosts(GATEWAY_IPS, expectedCommand);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failsWhenOnlyTheSecondaryGatewayHasTheWrongConfig(boolean managed) {
        when(sshJClientActions.executeSshCommandOnHosts(eq(GATEWAY_IPS), anyString())).thenReturn(outcomes("Failure"));

        assertThrows(TestFailException.class, () -> validate(managed));
    }

    @Test
    void failsWhenTheSecondaryGatewayIsUnreachable() {
        when(sshJClientActions.executeSshCommandOnHosts(eq(GATEWAY_IPS), anyString())).thenReturn(new SshCommandOutcomes(List.of(
                SshCommandOutcome.executed("10.0.0.1", 0, "Success"),
                SshCommandOutcome.unreachable("10.0.0.2", new IllegalStateException("unreachable")))));

        TestFailException exception = assertThrows(TestFailException.class, () -> validate(true));

        assertThat(exception.getMessage()).contains("10.0.0.2");
    }

    @Test
    void failsInsteadOfPassingWhenThereAreNoGateways() {
        response.setInstanceGroups(List.of());

        assertThrows(TestFailException.class, () -> validate(true));

        verifyNoInteractions(sshJClientActions);
    }

    private void validate(boolean managed) {
        if (managed) {
            underTest.validateFileContentExistsOnAllGateways(testDto, CONFIG_FILE, "^minimum_auth_version: 2$");
        } else {
            underTest.validateFileNotExistsOnAllGateways(testDto, CONFIG_FILE);
        }
    }

    private SshCommandOutcomes outcomes(String secondaryOutput) {
        return new SshCommandOutcomes(List.of(SshCommandOutcome.executed("10.0.0.1", 0, "Success"),
                SshCommandOutcome.executed("10.0.0.2", 0, secondaryOutput)));
    }

    private InstanceGroupV4Response group(InstanceGroupType type, InstanceMetaDataV4Response... instances) {
        InstanceGroupV4Response group = new InstanceGroupV4Response();
        group.setType(type);
        group.setMetadata(new LinkedHashSet<>(List.of(instances)));
        return group;
    }

    private InstanceMetaDataV4Response instance(String ip, InstanceMetadataType type) {
        InstanceMetaDataV4Response instance = new InstanceMetaDataV4Response();
        instance.setPrivateIp(ip);
        instance.setInstanceType(type);
        return instance;
    }
}
