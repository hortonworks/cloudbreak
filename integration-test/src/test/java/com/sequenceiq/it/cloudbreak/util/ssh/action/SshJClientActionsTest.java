package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
