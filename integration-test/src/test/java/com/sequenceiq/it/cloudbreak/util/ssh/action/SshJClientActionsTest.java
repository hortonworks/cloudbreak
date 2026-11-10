package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome.Result;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClientFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.userauth.UserAuthException;

@ExtendWith(MockitoExtension.class)
class SshJClientActionsTest {

    private static final String INSTANCE_IP = "10.0.0.1";

    private static final String COMMAND = "echo HelloWorld";

    private static final String HOST_GROUP = "master";

    private static final String STDERR_NOISE = """
            Traceback (most recent call last):
            AttributeError: 'IPCMessageClient' object has no attribute '_connecting_future'
            sudo: unable to resolve host master0: NOK
            """;

    @Mock
    private SshJClientFactory sshJClientFactory;

    @Mock
    private SshJClient sshJClient;

    @InjectMocks
    private SshJClientActions underTest;

    @Test
    void testExecuteCommandReturnsTheExecutedOutcome() throws IOException {
        SSHClient sshClient = mock(SSHClient.class);
        SshCommandOutcome executed = SshCommandOutcome.executed(INSTANCE_IP, 0, "HelloWorld");
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenReturn(sshClient);
        when(sshJClient.execute(sshClient, INSTANCE_IP, COMMAND)).thenReturn(executed);

        assertThat(underTest.executeSshCommand(INSTANCE_IP, COMMAND)).isSameAs(executed);
        verify(sshClient).close();
    }

    @Test
    void testExecuteCommandReportsRejectedCredentialsAsAuthFailure() throws IOException {
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenThrow(new UserAuthException("Exhausted available authentication"));

        assertThat(underTest.executeSshCommand(INSTANCE_IP, COMMAND).result()).isEqualTo(Result.AUTH_FAILURE);
    }

    @Test
    void testExecuteCommandReportsConnectTimeoutAsUnreachable() throws IOException {
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenThrow(new SocketTimeoutException("Connect timed out"));

        assertThat(underTest.executeSshCommand(INSTANCE_IP, COMMAND).result()).isEqualTo(Result.UNREACHABLE);
    }

    @Test
    void testExecuteCommandReportsOutputReadTimeoutAsTimedOut() throws IOException {
        SSHClient sshClient = mock(SSHClient.class);
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenReturn(sshClient);
        when(sshJClient.execute(sshClient, INSTANCE_IP, COMMAND)).thenThrow(new SocketTimeoutException("Timeout of 600000ms while waiting for data"));

        SshCommandOutcome outcome = underTest.executeSshCommand(INSTANCE_IP, COMMAND);

        assertThat(outcome.result()).isEqualTo(Result.TIMED_OUT);
        assertThat(outcome.executed()).isFalse();
        verify(sshClient).close();
    }

    @Test
    void testExecuteCommandReportsIoFailureDuringExecutionAsUnreachable() throws IOException {
        SSHClient sshClient = mock(SSHClient.class);
        ConnectionException cause = new ConnectionException("Timeout expired");
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenReturn(sshClient);
        when(sshJClient.execute(sshClient, INSTANCE_IP, COMMAND)).thenThrow(cause);

        SshCommandOutcome outcome = underTest.executeSshCommand(INSTANCE_IP, COMMAND);

        assertThat(outcome.result()).isEqualTo(Result.UNREACHABLE);
        assertThat(outcome.throwable()).isSameAs(cause);
        verify(sshClient).close();
    }

    @Test
    void testExecuteOnHostsRunsTheCommandOncePerDistinctInstance() throws IOException {
        SSHClient sshClient = mock(SSHClient.class);
        when(sshJClientFactory.createSshClient(eq(INSTANCE_IP), isNull(), isNull(), isNull())).thenReturn(sshClient);
        when(sshJClient.execute(sshClient, INSTANCE_IP, COMMAND)).thenReturn(SshCommandOutcome.executed(INSTANCE_IP, 0, "HelloWorld"));

        SshCommandOutcomes outcomes = underTest.executeSshCommandOnHosts(List.of(INSTANCE_IP, INSTANCE_IP), COMMAND);

        assertThat(outcomes).hasSize(1);
        verify(sshJClient, times(1)).execute(eq(sshClient), eq(INSTANCE_IP), anyString());
    }

    @Test
    void testExecuteCommandFailsWithTheCommandWhenTheInstanceIpIsBlank() {
        TestFailException exception = assertThrows(TestFailException.class, () -> underTest.executeSshCommand(null, COMMAND));

        assertThat(exception.getMessage()).contains(COMMAND).contains("instance IP is blank");
    }

    @Test
    void testCheckLoggingAgentStatusIgnoresTheUnmarkedOutput() throws IOException {
        mockCommandOutput(STDERR_NOISE + "LOGGING_AGENT_STATUS: databusAccessible OK\n");

        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThat(underTest.checkLoggingAgentStatus(testDto, instanceGroups(), List.of(HOST_GROUP))).isSameAs(testDto);
    }

    @Test
    void testCheckLoggingAgentStatusReportsOnlyTheMarkedStatuses() throws IOException {
        mockCommandOutput(STDERR_NOISE + "LOGGING_AGENT_STATUS: databusAccessible NOK\nLOGGING_AGENT_STATUS: computeMonitoringAccessible OK\n"
                + "LOGGING_AGENT_STATUS: s3Accessible NOK\n");

        TestFailException exception = assertThrows(TestFailException.class,
                () -> underTest.checkLoggingAgentStatus(mock(CloudbreakTestDto.class), instanceGroups(), List.of(HOST_GROUP)));

        assertThat(exception.getMessage())
                .contains("databusAccessible, s3Accessible")
                .doesNotContain("Traceback")
                .doesNotContain("computeMonitoringAccessible")
                .doesNotContain("LOGGING_AGENT_STATUS: ");
    }

    @Test
    void testCheckLoggingAgentStatusFailsWhenNoStatusIsMarked() throws IOException {
        mockCommandOutput(STDERR_NOISE);

        TestFailException exception = assertThrows(TestFailException.class,
                () -> underTest.checkLoggingAgentStatus(mock(CloudbreakTestDto.class), instanceGroups(), List.of(HOST_GROUP)));

        assertThat(exception.getMessage()).contains("Logging agent status cannot be checked");
    }

    @Test
    void testCheckLoggingAgentStatusRejectsAnEmptyReport() throws IOException {
        mockCommandOutput("");

        assertThrows(TestFailException.class,
                () -> underTest.checkLoggingAgentStatus(mock(CloudbreakTestDto.class), instanceGroups(), List.of(HOST_GROUP)));

        verify(sshJClient).execute(any(), eq(INSTANCE_IP), contains("if [ -z \"$STATUS\" ] ||"));
    }

    @Test
    void testCheckCdpServiceStatusIgnoresTheUnmarkedOutput() throws IOException {
        mockCommandOutput(STDERR_NOISE + "SERVICE_STATUS: salt-master OK\n");

        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThat(underTest.checkCdpServiceStatus(testDto, instanceGroups(), List.of(HOST_GROUP))).isSameAs(testDto);
    }

    @Test
    void testCheckCdpServiceStatusReportsOnlyTheMarkedNokStatuses() throws IOException {
        mockCommandOutput(STDERR_NOISE + "SERVICE_STATUS: salt-master OK\nSERVICE_STATUS: salt-minion NOK\n");

        TestFailException exception = assertThrows(TestFailException.class,
                () -> underTest.checkCdpServiceStatus(mock(CloudbreakTestDto.class), instanceGroups(), List.of(HOST_GROUP)));

        assertThat(exception.getMessage())
                .contains("salt-minion NOK")
                .doesNotContain("salt-master")
                .doesNotContain("unable to resolve host");
    }

    @Test
    void testCheckCdpServiceStatusFailsWhenNoStatusIsMarked() throws IOException {
        mockCommandOutput(STDERR_NOISE);

        TestFailException exception = assertThrows(TestFailException.class,
                () -> underTest.checkCdpServiceStatus(mock(CloudbreakTestDto.class), instanceGroups(), List.of(HOST_GROUP)));

        assertThat(exception.getMessage()).contains("status cannot be checked");
    }

    private void mockCommandOutput(String output) throws IOException {
        SSHClient sshClient = mock(SSHClient.class);
        when(sshJClientFactory.createSshClient(INSTANCE_IP, null, null, null)).thenReturn(sshClient);
        when(sshJClient.execute(eq(sshClient), eq(INSTANCE_IP), anyString())).thenReturn(SshCommandOutcome.executed(INSTANCE_IP, 0, output));
    }

    private List<InstanceGroupV4Response> instanceGroups() {
        InstanceMetaDataV4Response instanceMetaData = new InstanceMetaDataV4Response();
        instanceMetaData.setInstanceId("i-1");
        instanceMetaData.setPrivateIp(INSTANCE_IP);
        InstanceGroupV4Response instanceGroup = new InstanceGroupV4Response();
        instanceGroup.setName(HOST_GROUP);
        instanceGroup.setMetadata(Set.of(instanceMetaData));
        return List.of(instanceGroup);
    }
}
