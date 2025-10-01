package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@ExtendWith(MockitoExtension.class)
class SshSudoCommandActionsTest {

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    @Mock
    private SshJClient sshJClient;

    @InjectMocks
    private SshSudoCommandActions underTest;

    @Captor
    private ArgumentCaptor<String> hostCaptor;

    @Captor
    private ArgumentCaptor<String> commandCaptor;

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testExecuteCommand(boolean passwordSpecified) throws IOException {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String simpleCommand = "echo \"HelloWorld\"";
        String commandWithSubstitution = "variable=$(echo \"HelloWorld\")";
        String multilineCommand = """
                echo "HelloWorld"; \
                echo "SecondLine"\
                """;
        String expectedCommand = passwordSpecified ?
                "echo \"password\" | sudo -S bash -c \"echo \\\"HelloWorld\\\"\" && " +
                        "echo \"password\" | sudo -S bash -c \"variable=\\$(echo \\\"HelloWorld\\\")\" && " +
                        "echo \"password\" | sudo -S bash -c \"echo \\\"HelloWorld\\\"; echo \\\"SecondLine\\\"\"" :
                "sudo bash -c \"echo \\\"HelloWorld\\\"\" && " +
                        "sudo bash -c \"variable=\\$(echo \\\"HelloWorld\\\")\" && " +
                        "sudo bash -c \"echo \\\"HelloWorld\\\"; echo \\\"SecondLine\\\"\"";
        for (String ipAddress : ipAddresses) {
            SSHClient sshClient = mock();
            when(sshJClient.createSshClient(ipAddress, USER, passwordSpecified ? PASSWORD : null, null)).thenReturn(sshClient);
            when(sshJClient.execute(eq(sshClient), anyString())).thenReturn(Pair.of(0, "Success for " + ipAddress));
        }

        assertDoesNotThrow(() ->
                underTest.executeCommand(ipAddresses, USER, passwordSpecified ? PASSWORD : null, simpleCommand, commandWithSubstitution, multilineCommand));

        verify(sshJClient, times(ipAddresses.size())).createSshClient(hostCaptor.capture(), eq(USER), eq(passwordSpecified ? PASSWORD : null), eq(null));
        assertThat(hostCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(ipAddresses);
        verify(sshJClient, times(ipAddresses.size())).execute(any(SSHClient.class), commandCaptor.capture());
        assertThat(commandCaptor.getAllValues()).allMatch(expectedCommand::equals);
    }

    @Test
    void testExecuteCommandWhenACommandFails() throws IOException {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String failingIp = "11.11.11.11";
        String command = "echo \"HelloWorld\"";
        String expectedCommand = "sudo bash -c \"echo \\\"HelloWorld\\\"\"";
        for (String ipAddress : ipAddresses.subList(0, ipAddresses.indexOf(failingIp) + 1)) {
            SSHClient sshClient = mock();
            when(sshJClient.createSshClient(ipAddress, USER, null, null)).thenReturn(sshClient);
            if (failingIp.equals(ipAddress)) {
                when(sshJClient.execute(eq(sshClient), anyString())).thenReturn(Pair.of(1, "Failure for " + ipAddress));
            } else {
                when(sshJClient.execute(eq(sshClient), anyString())).thenReturn(Pair.of(0, "Success for " + ipAddress));
            }
        }

        assertThrows(TestFailException.class, () -> underTest.executeCommand(ipAddresses, USER, null, command),
                () -> "sudo command failed on '" + failingIp + "' for user '" + USER + "'. ");

        verify(sshJClient, times(2)).createSshClient(hostCaptor.capture(), eq(USER), eq(null), eq(null));
        assertThat(hostCaptor.getAllValues()).containsAnyElementsOf(ipAddresses.subList(0, 2));
        verify(sshJClient, times(2)).execute(any(SSHClient.class), commandCaptor.capture());
        assertThat(commandCaptor.getAllValues()).allMatch(expectedCommand::equals);
    }

    @Test
    void testExecuteCommandWithoutThrowing() throws IOException {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String failingIp = "11.11.11.11";
        String command = "echo \"HelloWorld\"";
        String expectedCommand = "sudo bash -c \"echo \\\"HelloWorld\\\"\"";
        for (String ipAddress : ipAddresses) {
            SSHClient sshClient = mock();
            when(sshJClient.createSshClient(ipAddress, null, null, null)).thenReturn(sshClient);
            if (failingIp.equals(ipAddress)) {
                when(sshJClient.execute(eq(sshClient), anyString())).thenReturn(Pair.of(1, "Failure for " + ipAddress));
            } else {
                when(sshJClient.execute(eq(sshClient), anyString())).thenReturn(Pair.of(0, "Success for " + ipAddress));
            }
        }

        Map<String, Pair<Integer, String>> results = assertDoesNotThrow(() -> underTest.executeCommandWithoutThrowing(ipAddresses, command));

        assertThat(results).hasSize(ipAddresses.size());
        assertThat(results.entrySet()).extracting(Map.Entry::getKey).containsExactlyInAnyOrderElementsOf(ipAddresses);
        assertThat(results.entrySet()).extracting(Map.Entry::getValue).containsExactlyInAnyOrderElementsOf(List.of(
                Pair.of(0, "Success for 10.10.10.10"),
                Pair.of(1, "Failure for 11.11.11.11"),
                Pair.of(0, "Success for 12.12.12.12")
        ));
    }
}
