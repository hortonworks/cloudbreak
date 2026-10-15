package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

@ExtendWith(MockitoExtension.class)
class SshJClientTest {

    private static final String INSTANCE_IP = "10.0.0.1";

    private static final String COMMAND = "echo HelloWorld";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SSHClient sshClient;

    @Mock
    private Session session;

    @Mock
    private Command command;

    private final SshJClient underTest = new SshJClient();

    @BeforeEach
    void setUp() throws IOException {
        when(sshClient.startSession()).thenReturn(session);
        when(session.exec(COMMAND)).thenReturn(command);
        when(command.getInputStream()).thenReturn(new ByteArrayInputStream("HelloWorld".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testExecuteReturnsExitStatusOutputAndGivenInstanceIp() throws IOException {
        when(command.getExitStatus()).thenReturn(2);

        SshCommandOutcome outcome = underTest.execute(sshClient, INSTANCE_IP, COMMAND);

        assertThat(outcome.executed()).isTrue();
        assertThat(outcome.instanceIp()).isEqualTo(INSTANCE_IP);
        assertThat(outcome.returnCode()).isEqualTo(2);
        assertThat(outcome.commandOutput()).isEqualTo("HelloWorld");
    }

    @Test
    void testExecuteMapsMissingExitStatusToFailure() throws IOException {
        when(command.getExitStatus()).thenReturn(null);

        SshCommandOutcome outcome = underTest.execute(sshClient, INSTANCE_IP, COMMAND);

        assertThat(outcome.executed()).isTrue();
        assertThat(outcome.returnCode()).isEqualTo(SshCommandOutcome.MISSING_RETURN_CODE);
        assertThat(outcome.returnCodeSuccess()).isFalse();
    }

    @Test
    void testExecuteRetriesStartingTheSession() throws IOException {
        when(sshClient.startSession()).thenThrow(new ConnectionException("Session open failed")).thenReturn(session);
        when(command.getExitStatus()).thenReturn(0);

        SshCommandOutcome outcome = underTest.execute(sshClient, INSTANCE_IP, COMMAND);

        assertThat(outcome.returnCode()).isZero();
        verify(sshClient, times(2)).startSession();
        verify(session, times(1)).exec(COMMAND);
    }

    @Test
    void testExecuteDoesNotRetryTheCommandOnceStarted() throws IOException {
        ConnectionException exitStatusTimeout = new ConnectionException("Timeout expired");
        doThrow(exitStatusTimeout).when(command).join(10L, TimeUnit.SECONDS);

        ConnectionException exception = assertThrows(ConnectionException.class, () -> underTest.execute(sshClient, INSTANCE_IP, COMMAND));

        assertThat(exception).isSameAs(exitStatusTimeout);
        verify(sshClient, times(1)).startSession();
        verify(session, times(1)).exec(COMMAND);
    }

    @Test
    void testExecuteWaitsForTheExitStatusForTheGivenTime() throws IOException {
        when(command.getExitStatus()).thenReturn(0);

        underTest.execute(sshClient, INSTANCE_IP, COMMAND, 120L);

        verify(command).join(120L, TimeUnit.SECONDS);
    }
}
